package com.smartsociety.service;

import java.awt.image.BufferedImage;
import java.util.UUID;

/**
 * GoF: Singleton Pattern
 * QRCodeService is a single shared service instance across the system.
 * Handles QR code generation (as strings) and decoding.
 * 
 * NOTE: For actual QR image generation, ZXing library is required.
 * For webcam scanning, Sarxos Webcam API is required.
 * This implementation generates/decodes QR code strings.
 * Full QR image and webcam integration works when libraries are present.
 */
public class QRCodeService {

    private static volatile QRCodeService instance;

    private QRCodeService() {
        System.out.println("[QRCodeService] Singleton instance created.");
    }

    /**
     * Returns the singleton instance (double-checked locking).
     */
    public static QRCodeService getInstance() {
        if (instance == null) {
            synchronized (QRCodeService.class) {
                if (instance == null) {
                    instance = new QRCodeService();
                }
            }
        }
        return instance;
    }

    /**
     * Generates a unique QR code string for an approval.
     * As per SD-01: generateQRCode(approvalID) → return qrCodeData
     *
     * @param approvalId the approval ID to encode
     * @return unique QR code string
     */
    public String generateQRCode(int approvalId) {
        String qrCode = "SSEMS-APR-" + approvalId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[QRCodeService] Generated QR code: " + qrCode);
        return qrCode;
    }

    /**
     * Generates a unique QR code string for a resident.
     * Used for resident gate entry/exit.
     *
     * @param residentId the resident user ID
     * @return unique resident QR code string
     */
    public String generateResidentQR(int residentId) {
        String qrCode = "SSEMS-RES-" + residentId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        System.out.println("[QRCodeService] Generated Resident QR: " + qrCode);
        return qrCode;
    }

    /**
     * Generates a graphical JavaFX Image of the given QR code string.
     */
    public javafx.scene.image.Image generateQRImage(String qrData) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);
            java.awt.image.BufferedImage bufferedImage = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(bitMatrix);
            return javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            System.err.println("[QRCodeService] Error generating graphical QR: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decodes a QR code string to extract the approval ID.
     * As per SD-07: decodeQRCode(qrData) → return approvalID
     *
     * @param qrData the QR code string
     * @return the approval ID, or -1 if invalid
     */
    public int decodeApprovalQR(String qrData) {
        try {
            if (qrData != null && qrData.startsWith("SSEMS-APR-")) {
                String[] parts = qrData.split("-");
                if (parts.length >= 3) {
                    return Integer.parseInt(parts[2]);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("[QRCodeService] Invalid QR code format: " + qrData);
        }
        return -1;
    }

    /**
     * Decodes a QR code string to extract the resident ID.
     * As per SD-10/11: verifyQRCode(qrData) → return residentID
     *
     * @param qrData the QR code string
     * @return the resident user ID, or -1 if invalid
     */
    public int decodeResidentQR(String qrData) {
        try {
            if (qrData != null && qrData.startsWith("SSEMS-RES-")) {
                String[] parts = qrData.split("-");
                if (parts.length >= 3) {
                    return Integer.parseInt(parts[2]);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("[QRCodeService] Invalid resident QR format: " + qrData);
        }
        return -1;
    }

    /**
     * Verifies if a QR code string is valid (properly formatted).
     */
    public boolean isValidQR(String qrData) {
        return qrData != null && (qrData.startsWith("SSEMS-APR-") || qrData.startsWith("SSEMS-RES-"));
    }

    /**
     * Determines the type of QR code (APPROVAL or RESIDENT).
     */
    public String getQRType(String qrData) {
        if (qrData == null) return "UNKNOWN";
        if (qrData.startsWith("SSEMS-APR-")) return "APPROVAL";
        if (qrData.startsWith("SSEMS-RES-")) return "RESIDENT";
        return "UNKNOWN";
    }

    /**
     * Invalidates an old QR code by marking it as used/expired.
     * As per SD-02: invalidateOldQR(oldQRCode)
     * In practice, this is handled by updating the DB record.
     */
    public boolean invalidateQR(String oldQRCode) {
        System.out.println("[QRCodeService] Invalidated QR: " + oldQRCode);
        return true;
    }

    /**
     * Attempts to scan a QR code using the laptop webcam synchronously.
     * Uses Sarxos Webcam API to capture a frame and ZXing to decode.
     * Falls back to null if webcam/libraries are unavailable.
     *
     * @return the decoded QR string, or null if scan failed
     */
    public String scanQRFromWebcam() {
        try {
            // Attempt to use Sarxos Webcam API
            com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (webcam == null) {
                System.err.println("[QRCodeService] No webcam found.");
                return null;
            }

            if (!webcam.isOpen()) {
                webcam.setViewSize(new java.awt.Dimension(320, 240));
                webcam.open();
            }

            BufferedImage image = webcam.getImage();
            if (image == null) {
                return null;
            }

            return decodeImage(image);

        } catch (NoClassDefFoundError e) {
            // Sarxos or ZXing libraries not available
            System.err.println("[QRCodeService] Webcam libraries not available. Use manual QR input.");
            return null;
        } catch (Exception e) {
            System.err.println("[QRCodeService] Webcam scan failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decodes a QR code from a given BufferedImage using ZXing.
     * @param image the BufferedImage to decode
     * @return the decoded text or null if not found
     */
    public String decodeImage(BufferedImage image) {
        if (image == null) return null;
        try {
            com.google.zxing.LuminanceSource source =
                    new com.google.zxing.client.j2se.BufferedImageLuminanceSource(image);
            com.google.zxing.BinaryBitmap bitmap =
                    new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));

            com.google.zxing.Result result =
                    new com.google.zxing.MultiFormatReader().decode(bitmap);

            String decoded = result.getText();
            System.out.println("[QRCodeService] Scanned QR from image: " + decoded);
            return decoded;
        } catch (com.google.zxing.NotFoundException e) {
            // No QR code found in the image, expected in continuous scanning
            return null;
        } catch (Exception e) {
            // Other decoding exceptions
            return null;
        }
    }

    /**
     * Gets a BufferedImage of the webcam feed for display purposes.
     * Returns null if webcam is not available.
     */
    public BufferedImage getWebcamFrame() {
        try {
            com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (webcam != null) {
                if (!webcam.isOpen()) {
                    webcam.setViewSize(new java.awt.Dimension(320, 240));
                    webcam.open();
                }
                return webcam.getImage();
            }
        } catch (NoClassDefFoundError e) {
            // Libraries not available
        } catch (Exception e) {
            System.err.println("[QRCodeService] Error getting webcam frame: " + e.getMessage());
        }
        return null;
    }

    /**
     * Closes the webcam if it is open.
     */
    public void closeWebcam() {
        try {
            com.github.sarxos.webcam.Webcam webcam = com.github.sarxos.webcam.Webcam.getDefault();
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        } catch (NoClassDefFoundError e) {
            // Libraries not available
        } catch (Exception e) {
            System.err.println("[QRCodeService] Error closing webcam: " + e.getMessage());
        }
    }
}
