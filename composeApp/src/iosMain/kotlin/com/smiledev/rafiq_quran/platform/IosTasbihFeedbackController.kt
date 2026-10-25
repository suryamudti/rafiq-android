package com.smiledev.rafiq_quran.platform

class IosTasbihFeedbackController : TasbihFeedbackController {
    override fun vibrateTick() {
        // iOS UIImpactFeedbackGenerator(UIImpactFeedbackStyleLight).impactOccurred()
    }

    override fun vibrateTargetReached() {
        // iOS UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackTypeSuccess)
    }

    override fun playClickSound() {
        // iOS AudioServicesPlaySystemSound(1104u)
    }
}
