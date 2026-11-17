package com.example.game

import androidx.compose.ui.geometry.Offset
import com.example.data.GestureSkillId
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

data class RecognizedGesture(
    val skill: GestureSkillId,
    val dirX: Float,
    val dirY: Float
)

object GestureRecognizer {
    fun classify(
        points: List<Offset>,
        holdDurationMs: Long,
        unlockedGestures: Set<String>
    ): RecognizedGesture {
        if (points.isEmpty()) {
            return RecognizedGesture(GestureSkillId.TAP_STRIKE, 1f, 0f)
        }

        val start = points.first()
        val end = points.last()
        var pathLen = 0f
        var minX = start.x
        var maxX = start.x
        var minY = start.y
        var maxY = start.y

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val cur = points[i]
            pathLen += hypot(cur.x - prev.x, cur.y - prev.y)
            if (cur.x < minX) minX = cur.x
            if (cur.x > maxX) maxX = cur.x
            if (cur.y < minY) minY = cur.y
            if (cur.y > maxY) maxY = cur.y
        }

        val spanX = maxX - minX
        val spanY = maxY - minY
        val diag = max(1f, hypot(spanX, spanY))
        val dx = end.x - start.x
        val dy = end.y - start.y
        val endDist = hypot(dx, dy)
        val absDx = abs(dx)
        val absDy = abs(dy)
        val mag = max(1f, endDist)

        // 1. Hold & Release Ultimate
        if (holdDurationMs >= 380L && diag < 55f && GestureSkillId.HOLD_ULTIMATE.id in unlockedGestures) {
            return RecognizedGesture(GestureSkillId.HOLD_ULTIMATE, 0f, -1f)
        }

        // 2. Tap Strike
        if (pathLen < 32f && diag < 26f) {
            return RecognizedGesture(GestureSkillId.TAP_STRIKE, 1f, 0f)
        }

        // 3. Circle / Loop Detection (angular sweep around centroid + closedness)
        if (points.size >= 8 && pathLen >= 130f && diag >= 45f &&
            GestureSkillId.CIRCLE_NOVA.id in unlockedGestures
        ) {
            val cx = (minX + maxX) / 2f
            val cy = (minY + maxY) / 2f
            var sweep = 0f
            var prevAngle = atan2(points[0].y - cy, points[0].x - cx)
            for (i in 2 until points.size step 2) {
                val a = atan2(points[i].y - cy, points[i].x - cx)
                var da = a - prevAngle
                while (da > Math.PI) da -= (2 * Math.PI).toFloat()
                while (da < -Math.PI) da += (2 * Math.PI).toFloat()
                sweep += da
                prevAngle = a
            }
            if (abs(sweep) >= 3.4f || (endDist < diag * 0.55f && pathLen > diag * 2.1f)) {
                return RecognizedGesture(GestureSkillId.CIRCLE_NOVA, 0f, 0f)
            }
        }

        // 4. Z / Zigzag Detection (direction reversals)
        if (points.size >= 6 && pathLen >= 110f && GestureSkillId.Z_SLASH.id in unlockedGestures) {
            var sharpTurns = 0
            var lastVx = 0f
            var lastVy = 0f
            for (i in 2 until points.size step 2) {
                val vx = points[i].x - points[i - 2].x
                val vy = points[i].y - points[i - 2].y
                val vMag = hypot(vx, vy)
                if (vMag > 8f) {
                    if (lastVx != 0f || lastVy != 0f) {
                        val lastMag = hypot(lastVx, lastVy)
                        val dot = (vx * lastVx + vy * lastVy) / (vMag * lastMag)
                        if (dot < 0.25f) sharpTurns++
                    }
                    lastVx = vx
                    lastVy = vy
                }
            }
            if (sharpTurns >= 2 && pathLen > endDist * 1.45f) {
                return RecognizedGesture(GestureSkillId.Z_SLASH, dx / mag, dy / mag)
            }
        }

        // 5. Downward Swipe -> Shield Parry & Deflect Stance (Defensive Maneuver!)
        if (dy > 0f && absDy >= 44f && absDy >= absDx * 1.25f &&
            GestureSkillId.DOWN_PARRY.id in unlockedGestures
        ) {
            return RecognizedGesture(
                skill = GestureSkillId.DOWN_PARRY,
                dirX = if (absDx > 12f) dx / mag else 0f,
                dirY = 1f
            )
        }

        // 6. Upward Swipe -> Skyward Uppercut Sword Slash
        if (dy < 0f && absDy >= 48f && absDy >= absDx * 1.35f &&
            GestureSkillId.UP_SLAM.id in unlockedGestures
        ) {
            return RecognizedGesture(GestureSkillId.UP_SLAM, if (absDx > 12f) dx / mag else 0f, -1f)
        }

        // 7. Diagonal Upward Swipe -> Rising Crescent Sword Cleave
        if (dy < 0f && absDx >= 30f && absDy >= 30f &&
            absDy in (absDx * 0.48f)..(absDx * 1.35f) &&
            GestureSkillId.DIAGONAL_SLASH.id in unlockedGestures
        ) {
            return RecognizedGesture(GestureSkillId.DIAGONAL_SLASH, dx / mag, dy / mag)
        }

        // 8. Left / Right Directional Swipe -> Horizontal Sword Lunge
        return RecognizedGesture(GestureSkillId.SWIPE_DASH, dx / mag, dy / mag)
    }
}
