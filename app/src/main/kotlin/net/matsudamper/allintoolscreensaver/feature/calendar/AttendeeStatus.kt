package net.matsudamper.allintoolscreensaver.feature.calendar

enum class AttendeeStatus {
    UNKNOWN,

    /**
     * 承諾
     */
    ACCEPTED,

    /**
     * 辞退
     */
    DECLINED,

    /**
     *未回答
     */
    INVITED,

    /**
     * 仮承諾
     */
    TENTATIVE,
}
