package zohar.com.ndn_liteble.utils;

/**
 * 常量类
 */
public class Constant {

    public static final int REQUSET_QR = 100; // 开启二维码扫码请求
    public static final String QR_RESULT = "rq_result"; // 二维码扫码结果
    public static final int REQUSET_BLE_CODE = 111; // 蓝牙请求


    //权限请求码
    public static final int PERMISSION_CAMER = 2000; //相机权限请求码
    public static final int PERMISSION_WIRTE_STORAGE = 201;// 写存储权限
    public static final int REQUEST_COARSE_LOCATION = 202; // 粗精度位置权限
    public static final int REQUEST_CAMER_PERMISSION = 20003; // 请求相机权限码

    // for the ecc_256 variant of sign on, device identifier is 12 bytes
    public static byte[] DEVICE_IDENTIFIER_1 = new byte[] {
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
    };
    public static byte[] DEVICE_IDENTIFIER_2 = new byte[] {
            0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02,
    };

    // these are the raw key bytes of the ecc public key (including)
    // the point identifier, which is the first byte
    public static byte[] BOOTSTRAP_ECC_PUBLIC_NO_POINT_IDENTIFIER = new byte[] {
            (byte) 0x41, (byte) 0xA0, (byte) 0x02, (byte) 0x0C, (byte) 0x65, (byte) 0xCA, (byte) 0x1B, (byte) 0xD0,
            (byte) 0xB4, (byte) 0x4B, (byte) 0x0B, (byte) 0xC9, (byte) 0xD3, (byte) 0x92, (byte) 0xE2, (byte) 0x14,
            (byte) 0xDB, (byte) 0x7A, (byte) 0x97, (byte) 0xC3, (byte) 0x22, (byte) 0xEA, (byte) 0xC7, (byte) 0xD7,
            (byte) 0xEA, (byte) 0x05, (byte) 0x77, (byte) 0xFB, (byte) 0x74, (byte) 0x4C, (byte) 0xC0, (byte) 0x86,
            (byte) 0x8F, (byte) 0xA6, (byte) 0xF9, (byte) 0x21, (byte) 0x72, (byte) 0x38, (byte) 0x92, (byte) 0xF3,
            (byte) 0x69, (byte) 0xA9, (byte) 0xAA, (byte) 0x82, (byte) 0xE0, (byte) 0xEC, (byte) 0x69, (byte) 0x77,
            (byte) 0x59, (byte) 0xA8, (byte) 0x6C, (byte) 0x5E, (byte) 0x7D, (byte) 0x74, (byte) 0x96, (byte) 0x1D,
            (byte) 0xB9, (byte) 0xCD, (byte) 0x9A, (byte) 0x3D, (byte) 0xC0, (byte) 0x2F, (byte) 0x86, (byte) 0x4A
    };

    // for the ecc_256 variant of sign on, the secure sign on code is 16 bytes
    public static byte[] SECURE_SIGN_ON_CODE = new byte[] {
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE,
            0x3D, 0x02, 0x01, 0x06, 0x08, 0x2A,
    };
}
