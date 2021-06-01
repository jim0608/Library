package com.mysafe.lib_identification;

/**
 * 静态属性
 */
public class Constants {
    /**
     * 虹软APP_ID
     */
    public static final String ARC_APP_ID = "GmNP65iNArFrr5i4vK4yB1Fegeg6TqyLeyvpZbBw3qJe";
    /**
     * 虹软SDK的KEY
     * 试用版./2021-01-18到期
     */
    public static final String ARC_SDK_KEY = "9bq1v9TtCe8GdxLKNHXEzoysLdGnrTDKF6VUKJX8srB2";

    /**
     * 激活码
     * 试用码:
     * 85T1-1143-U13E-9H2C 华为金手机
     * 85T1-1143-U12M-HHTJ 佳维视设备
     * 085T-1143-U15M-ETHH 人脸识别端
     * 085T-1143-U143-1AQ8 自助终端激活
     */
    public static final String ARC_ACTIVE_KEY_PUBLIC = "085T-1143-U143-1AQ8";
    public static final String ARC_ACTIVE_KEY_CUSTOM = "085T-1143-U167-K3QR";

    /**
     * 是否是第一次使用这个设备
     * 如果是,则激活引擎
     * 为了避免多次激活导致的失败
     */
    public static final boolean ARC_IS_FIRST_ACTIVE = true;
}
