package zohar.com.ndn_liteble.model;

/**
 * nRF52840板子model
 */
public class Board {

    private String macAddress; // 板子的mac地址
    private String identifierHex; // 板子的识别号
    private String KDPubCertificate; // 板子的KDPubCertificate

    public String getIdentifierHex() {
        return identifierHex;
    }

    public void setIdentifierHex(String identifierHex) {
        this.identifierHex = identifierHex;
    }

    public String getKDPubCertificate() {
        return KDPubCertificate;
    }

    public void setKDPubCertificate(String KDPubCertificate) {
        this.KDPubCertificate = KDPubCertificate;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
