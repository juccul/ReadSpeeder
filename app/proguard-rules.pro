# PDFBox probes these optional decoders and public-key encryption classes at runtime.
# ReadSpeeder imports ordinary PDFs and rejects encrypted PDFs before those paths run.
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.cert.**
-dontwarn org.bouncycastle.cms.**
