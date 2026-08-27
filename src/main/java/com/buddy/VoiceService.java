package com.buddy;

import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

import java.util.Locale;
import java.util.function.Consumer;

public class VoiceService {

    // =========================================================
    // SETTINGS
    // =========================================================

    private static final String MODEL_PATH =
            "src/main/resources/vosk-model-small-en-us-0.15";

    private static final float SAMPLE_RATE = 16000.0f;

    private static final String WAKE_WORD = "wake up buddy";

    // Time given to Buddy to finish saying:
    // "Yes Sir, what's on your mind today?"
    private static final long WAKE_RESPONSE_DELAY = 2500;


    // =========================================================
    // VARIABLES
    // =========================================================

    private Model model;

    private TargetDataLine microphone;

    private volatile boolean listening = false;

    private volatile boolean waitingForWakeWord = true;

    private volatile boolean acceptingCommand = false;

    private Thread listeningThread;

    private Consumer<String> commandCallback;

    private Runnable activationCallback;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public VoiceService() {

        System.out.println();
        System.out.println("======================================");
        System.out.println("        BUDDY VOICE SERVICE");
        System.out.println("======================================");

        try {

            System.out.println("Loading Vosk model...");

            model = new Model(MODEL_PATH);

            System.out.println("Vosk model loaded successfully.");

        } catch (Exception e) {

            System.out.println();
            System.out.println("======================================");
            System.out.println("       VOSK MODEL ERROR");
            System.out.println("======================================");

            System.out.println(
                    "Model path: " + MODEL_PATH
            );

            System.out.println(
                    "Error: " + e.getMessage()
            );

            e.printStackTrace();

            model = null;
        }
    }


    // =========================================================
    // ACTIVATION CALLBACK
    // =========================================================

    public void setActivationCallback(
            Runnable callback
    ) {

        this.activationCallback = callback;
    }


    // =========================================================
    // COMMAND CALLBACK
    // =========================================================

    public void setCommandCallback(
            Consumer<String> callback
    ) {

        this.commandCallback = callback;
    }


    // =========================================================
    // START LISTENING
    // =========================================================

    public synchronized void startListening() {

        if (listening) {

            System.out.println(
                    "VoiceService is already running."
            );

            return;
        }

        if (model == null) {

            System.out.println();
            System.out.println(
                    "VOICE ERROR: Vosk model is not loaded."
            );

            return;
        }

        listening = true;

        waitingForWakeWord = true;

        acceptingCommand = false;

        listeningThread =
                new Thread(
                        this::listenLoop,
                        "Buddy-Microphone"
                );

        listeningThread.setDaemon(true);

        listeningThread.start();

        System.out.println();
        System.out.println("======================================");
        System.out.println("     BUDDY MICROPHONE STARTING");
        System.out.println("======================================");
    }


    // =========================================================
    // MICROPHONE LOOP
    // =========================================================

    private void listenLoop() {

        AudioFormat format =
                new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        SAMPLE_RATE,
                        16,
                        1,
                        2,
                        SAMPLE_RATE,
                        false
                );


        DataLine.Info info =
                new DataLine.Info(
                        TargetDataLine.class,
                        format
                );


        try {

            // -------------------------------------------------
            // CHECK MICROPHONE
            // -------------------------------------------------

            if (!AudioSystem.isLineSupported(info)) {

                System.out.println();
                System.out.println(
                        "VOICE ERROR: Microphone format not supported."
                );

                listening = false;

                return;
            }


            // -------------------------------------------------
            // OPEN MICROPHONE
            // -------------------------------------------------

            microphone =
                    (TargetDataLine)
                            AudioSystem.getLine(info);


            microphone.open(format);

            microphone.start();


            System.out.println();
            System.out.println("======================================");
            System.out.println("          MICROPHONE: ON");
            System.out.println("       VOICE SYSTEM: READY");
            System.out.println("======================================");

            System.out.println();
            System.out.println(
                    "Say: Wake Up Buddy"
            );


            // -------------------------------------------------
            // VOSK
            // -------------------------------------------------

            try (
                    Recognizer recognizer =
                            new Recognizer(
                                    model,
                                    SAMPLE_RATE
                            )
            ) {

                byte[] buffer =
                        new byte[4096];


                while (listening) {

                    int bytesRead =
                            microphone.read(
                                    buffer,
                                    0,
                                    buffer.length
                            );


                    if (bytesRead <= 0) {

                        continue;
                    }


                    boolean finalResult =
                            recognizer.acceptWaveForm(
                                    buffer,
                                    bytesRead
                            );


                    /*
                     * IMPORTANT:
                     *
                     * We ONLY process FINAL results.
                     *
                     * Do NOT process getPartialResult().
                     *
                     * This prevents duplicate commands.
                     */

                    if (!finalResult) {

                        continue;
                    }


                    String text =
                            extractText(
                                    recognizer.getResult()
                            );


                    if (
                            text == null
                                    || text.trim().isEmpty()
                    ) {

                        continue;
                    }


                    processSpeech(text);
                }
            }


        } catch (Exception e) {

            System.out.println();
            System.out.println("======================================");
            System.out.println("        MICROPHONE ERROR");
            System.out.println("======================================");

            System.out.println(
                    e.getClass().getName()
            );

            System.out.println(
                    e.getMessage()
            );

            e.printStackTrace();

            listening = false;

        } finally {

            closeMicrophone();
        }
    }


    // =========================================================
    // PROCESS SPEECH
    // =========================================================

    private void processSpeech(
            String text
    ) {

        if (
                text == null
                        || text.trim().isEmpty()
        ) {

            return;
        }


        String command =
                text
                        .trim()
                        .toLowerCase(Locale.ROOT);


        System.out.println(
                "HEARD: " + command
        );


        // =====================================================
        // WAITING FOR WAKE WORD
        // =====================================================

        if (waitingForWakeWord) {

            if (isWakeWord(command)) {

                activateBuddy();
            }

            return;
        }


        // =====================================================
        // BUDDY IS ACTIVE
        // =====================================================

        if (acceptingCommand) {

            System.out.println();
            System.out.println(
                    "USER COMMAND: " + command
            );


            acceptingCommand = false;


            if (commandCallback != null) {

                commandCallback.accept(command);

            } else {

                System.out.println(
                        "WARNING: Command callback is not connected."
                );
            }


            /*
             * After one command, go back to waiting
             * for "Wake Up Buddy".
             *
             * This prevents Buddy from continuously
             * interpreting random microphone noise.
             */

            waitingForWakeWord = true;

            System.out.println();
            System.out.println(
                    "Say: Wake Up Buddy"
            );
        }
    }


    // =========================================================
    // CHECK WAKE WORD
    // =========================================================

    private boolean isWakeWord(
            String command
    ) {

        if (command == null) {

            return false;
        }


        String text =
                command
                        .toLowerCase(Locale.ROOT)
                        .trim();


        return
                text.contains("wake up buddy")
                        || text.contains("wake buddy")
                        || text.contains("wake up body")
                        || text.contains("wake up baddy")
                        || text.contains("wake buddy");
    }


    // =========================================================
    // ACTIVATE BUDDY
    // =========================================================

    private synchronized void activateBuddy() {

        if (!waitingForWakeWord) {

            return;
        }


        System.out.println();
        System.out.println("======================================");
        System.out.println("         WAKE WORD DETECTED");
        System.out.println("======================================");


        // -----------------------------------------------------
        // Stop accepting wake words
        // -----------------------------------------------------

        waitingForWakeWord = false;

        acceptingCommand = false;


        // -----------------------------------------------------
        // Tell BuddyApp
        // -----------------------------------------------------

        if (activationCallback != null) {

            activationCallback.run();
        }


        /*
         * IMPORTANT:
         *
         * BuddyApp now says:
         *
         * "Yes Sir, what's on your mind today?"
         *
         * We wait for the voice to finish before accepting
         * the user's command.
         *
         * This prevents the microphone from hearing Buddy's
         * own voice and sending it to OpenAI.
         */

        Thread activationDelay =
                new Thread(
                        () -> {

                            try {

                                Thread.sleep(
                                        WAKE_RESPONSE_DELAY
                                );

                            } catch (
                                    InterruptedException e
                            ) {

                                Thread.currentThread()
                                        .interrupt();

                                return;
                            }


                            if (!listening) {

                                return;
                            }


                            acceptingCommand = true;


                            System.out.println();
                            System.out.println(
                                    "======================================"
                            );

                            System.out.println(
                                    "       BUDDY IS LISTENING"
                            );

                            System.out.println(
                                    "======================================"
                            );

                            System.out.println(
                                    "Speak your command now..."
                            );
                        },
                        "Buddy-Activation-Delay"
                );


        activationDelay.setDaemon(true);

        activationDelay.start();
    }


    // =========================================================
    // EXTRACT TEXT FROM VOSK JSON
    // =========================================================

    private String extractText(
            String json
    ) {

        if (
                json == null
                        || json.isEmpty()
        ) {

            return "";
        }


        String key = "\"text\"";


        int keyPosition =
                json.indexOf(key);


        if (keyPosition < 0) {

            return "";
        }


        int colon =
                json.indexOf(
                        ':',
                        keyPosition
                );


        if (colon < 0) {

            return "";
        }


        int firstQuote =
                json.indexOf(
                        '"',
                        colon + 1
                );


        if (firstQuote < 0) {

            return "";
        }


        int secondQuote =
                json.indexOf(
                        '"',
                        firstQuote + 1
                );


        if (secondQuote < 0) {

            return "";
        }


        return json.substring(
                firstQuote + 1,
                secondQuote
        );
    }


    // =========================================================
    // STOP LISTENING
    // =========================================================

    public synchronized void stopListening() {

        listening = false;

        waitingForWakeWord = true;

        acceptingCommand = false;


        closeMicrophone();


        if (
                listeningThread != null
        ) {

            listeningThread.interrupt();

            listeningThread = null;
        }


        System.out.println(
                "Buddy voice system stopped."
        );
    }


    // =========================================================
    // CLOSE MICROPHONE
    // =========================================================

    private synchronized void closeMicrophone() {

        try {

            if (microphone != null) {

                if (microphone.isRunning()) {

                    microphone.stop();
                }


                if (microphone.isOpen()) {

                    microphone.close();
                }


                microphone = null;
            }

        } catch (Exception e) {

            System.out.println(
                    "Microphone close error: "
                            + e.getMessage()
            );
        }
    }
}