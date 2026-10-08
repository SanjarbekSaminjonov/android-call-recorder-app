package uz.developer.privaterecorder;

interface IRecorderService {
    void startRecording(String outputFilePath);
    void stopRecording();
    boolean isRecording();
}
