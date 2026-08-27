package com.buddy;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.util.Random;
import java.util.concurrent.CompletableFuture;


/**
 * ============================================================
 *                         BUDDY AI
 * ============================================================
 *
 * Local futuristic personal AI assistant.
 *
 * AI:
 *     Ollama + llama3.2
 *
 * SPEECH TO TEXT:
 *     Vosk through VoiceService
 *
 * TEXT TO SPEECH:
 *     macOS "say" command
 *
 * WAKE WORD:
 *     Wake Up Buddy
 *
 * TIMEZONE:
 *     Asia/Kolkata
 *
 * ============================================================
 */
public class BuddyApp extends Application {


    // =========================================================
    // WINDOW
    // =========================================================

    private static final double WIDTH = 1200;

    private static final double HEIGHT = 800;


    // =========================================================
    // PARTICLES
    // =========================================================

    private static final int PARTICLE_COUNT = 2500;

    private final Random random =
            new Random();

    private final Particle[] particles =
            new Particle[PARTICLE_COUNT];


    // =========================================================
    // JAVAFX
    // =========================================================

    private Canvas canvas;

    private GraphicsContext gc;

    private AnimationTimer animationTimer;


    // =========================================================
    // VOICE SERVICE
    // =========================================================

    private VoiceService voiceService;


    // =========================================================
    // STATE
    // =========================================================

    private volatile BuddyState state =
            BuddyState.STANDBY;


    private volatile boolean speaking =
            false;


    private volatile boolean processing =
            false;


    // =========================================================
    // ANIMATION VARIABLES
    // =========================================================

    private double animationTime =
            0.0;

    private double energy =
            0.25;

    private double pulse =
            0.0;


    // =========================================================
    // DISPLAY TEXT
    // =========================================================

    private volatile String statusText =
            "SYSTEM STANDBY";


    private volatile String commandText =
            "WAKE UP BUDDY";


    private volatile String responseText =
            "";


    // =========================================================
    // OLLAMA
    // =========================================================

    private static final String OLLAMA_URL =
            "http://localhost:11434/api/chat";


    private static final String OLLAMA_MODEL =
            "llama3.2";


    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .version(
                            HttpClient.Version.HTTP_1_1
                    )
                    .build();


    // =========================================================
    // STATES
    // =========================================================

    private enum BuddyState {

        STANDBY,

        WAKE_DETECTED,

        ACTIVATING,

        LISTENING,

        THINKING,

        SPEAKING
    }


    // =========================================================
    // START APPLICATION
    // =========================================================

    @Override
    public void start(Stage stage) {

        System.out.println();

        System.out.println(
                "======================================"
        );

        System.out.println(
                "          BUDDY AI STARTING"
        );

        System.out.println(
                "======================================"
        );


        // -----------------------------------------------------
        // CREATE CANVAS
        // -----------------------------------------------------

        canvas =
                new Canvas(
                        WIDTH,
                        HEIGHT
                );


        gc =
                canvas.getGraphicsContext2D();


        // -----------------------------------------------------
        // CREATE SCENE
        // -----------------------------------------------------

        javafx.scene.Group root =
                new javafx.scene.Group(
                        canvas
                );


        Scene scene =
                new Scene(
                        root,
                        WIDTH,
                        HEIGHT,
                        Color.BLACK
                );


        // -----------------------------------------------------
        // STAGE
        // -----------------------------------------------------

        stage.setTitle(
                "BUDDY AI"
        );


        stage.setScene(
                scene
        );


        stage.setWidth(
                WIDTH
        );


        stage.setHeight(
                HEIGHT
        );


        stage.setResizable(
                false
        );


        stage.show();


        // -----------------------------------------------------
        // INITIALIZE PARTICLES
        // -----------------------------------------------------

        createParticles();


        // -----------------------------------------------------
        // START FUTURISTIC ANIMATION
        // -----------------------------------------------------

        startAnimation();


        // -----------------------------------------------------
        // INITIALIZE VOICE
        // -----------------------------------------------------

        initializeVoice();


        // -----------------------------------------------------
        // STARTUP INFORMATION
        // -----------------------------------------------------

        System.out.println();

        System.out.println(
                "======================================"
        );

        System.out.println(
                "       BUDDY AI SERVICE READY"
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "Local AI: Ollama"
        );

        System.out.println(
                "Model: llama3.2"
        );

        System.out.println(
                "Voice: Vosk"
        );

        System.out.println(
                "TTS: macOS Samantha"
        );

        System.out.println(
                "Wake word: Wake Up Buddy"
        );

        System.out.println(
                "Timezone: Asia/Kolkata"
        );

        System.out.println(
                "======================================"
        );


        // -----------------------------------------------------
        // TEST OLLAMA
        // -----------------------------------------------------

        testOllama();


        // -----------------------------------------------------
        // KEYBOARD CONTROLS
        // -----------------------------------------------------

        scene.setOnKeyPressed(
                event -> {

                    switch (
                            event.getCode()
                    ) {

                        case SPACE:

                            if (
                                    state
                                            == BuddyState.STANDBY
                            ) {

                                activateBuddy();
                            }

                            break;


                        case ESCAPE:

                            resetToStandby();

                            break;


                        default:

                            break;
                    }
                }
        );


        // -----------------------------------------------------
        // FOCUS
        // -----------------------------------------------------

        canvas.requestFocus();
    }


    // =========================================================
    // INITIALIZE VOICE
    // =========================================================

    private void initializeVoice() {

        try {

            voiceService =
                    new VoiceService();


            // -------------------------------------------------
            // WAKE WORD
            // -------------------------------------------------

            voiceService.setActivationCallback(
                    this::activateBuddy
            );


            // -------------------------------------------------
            // USER COMMAND
            // -------------------------------------------------

            voiceService.setCommandCallback(
                    this::processCommand
            );


            // -------------------------------------------------
            // START MICROPHONE
            // -------------------------------------------------

            voiceService.startListening();


        } catch (
                Exception e
        ) {

            System.out.println();

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "       VOICE INITIALIZATION ERROR"
            );

            System.out.println(
                    "======================================"
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // ACTIVATE BUDDY
    // =========================================================

    private synchronized void activateBuddy() {

        if (
                state
                        != BuddyState.STANDBY
        ) {

            return;
        }


        if (
                speaking
                        || processing
        ) {

            return;
        }


        System.out.println();

        System.out.println(
                "======================================"
        );

        System.out.println(
                "        WAKE WORD DETECTED"
        );

        System.out.println(
                "======================================"
        );


        state =
                BuddyState.WAKE_DETECTED;


        statusText =
                "WAKE WORD DETECTED";


        commandText =
                "BUDDY ACTIVATED";


        // -----------------------------------------------------
        // ACTIVATION UI
        // -----------------------------------------------------

        Platform.runLater(
                () -> {

                    state =
                            BuddyState.ACTIVATING;

                    statusText =
                            "INITIALIZING";

                    commandText =
                            "BUDDY ONLINE";
                }
        );


        // -----------------------------------------------------
        // TIME BASED GREETING
        // -----------------------------------------------------

        String greeting =
                getTimeBasedGreeting();


        // -----------------------------------------------------
        // SPEAK GREETING
        // -----------------------------------------------------

        speakAsync(
                greeting,
                false
        );


        // -----------------------------------------------------
        // MOVE TO LISTENING
        // -----------------------------------------------------

        CompletableFuture.runAsync(
                () -> {

                    try {

                        Thread.sleep(
                                1300
                        );

                    } catch (
                            InterruptedException e
                    ) {

                        Thread.currentThread()
                                .interrupt();
                    }


                    Platform.runLater(
                            () -> {

                                if (
                                        state
                                                != BuddyState.STANDBY
                                ) {

                                    state =
                                            BuddyState.LISTENING;

                                    statusText =
                                            "LISTENING";

                                    commandText =
                                            "SPEAK YOUR COMMAND";


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
                                }
                            }
                    );
                }
        );
    }


    // =========================================================
    // TIME BASED GREETING
    // =========================================================

    private String getTimeBasedGreeting() {

        ZonedDateTime indiaTime =
                ZonedDateTime.now(
                        ZoneId.of(
                                "Asia/Kolkata"
                        )
                );


        LocalTime time =
                indiaTime.toLocalTime();


        int hour =
                time.getHour();


        // -----------------------------------------------------
        // AFTER MIDNIGHT
        // -----------------------------------------------------

        if (
                hour >= 0
                        && hour < 5
        ) {

            return
                    "You're awake late, Sir. Are you up to something?";
        }


        // -----------------------------------------------------
        // MORNING
        // -----------------------------------------------------

        if (
                hour >= 5
                        && hour < 12
        ) {

            return
                    "Good morning, Sir. I'm awake and ready.";
        }


        // -----------------------------------------------------
        // AFTERNOON
        // -----------------------------------------------------

        if (
                hour >= 12
                        && hour < 17
        ) {

            return
                    "Good afternoon, Sir. I'm ready when you are.";
        }


        // -----------------------------------------------------
        // EVENING
        // -----------------------------------------------------

        return
                "Good evening, Sir. What can I do for you?";
    }


    // =========================================================
    // PROCESS COMMAND
    // =========================================================

    private void processCommand(
            String command
    ) {

        if (
                command == null
                        || command.trim().isEmpty()
        ) {

            return;
        }


        // -----------------------------------------------------
        // DO NOT LISTEN TO OWN VOICE
        // -----------------------------------------------------

        if (
                speaking
        ) {

            System.out.println(
                    "Ignoring microphone input while Buddy speaks."
            );

            return;
        }


        // -----------------------------------------------------
        // DO NOT PROCESS WHILE THINKING
        // -----------------------------------------------------

        if (
                processing
        ) {

            System.out.println(
                    "Buddy is already processing a command."
            );

            return;
        }


        // -----------------------------------------------------
        // ONLY LISTEN WHEN ACTIVE
        // -----------------------------------------------------

        if (
                state
                        != BuddyState.LISTENING
        ) {

            System.out.println(
                    "Ignoring command because Buddy is not listening."
            );

            return;
        }


        // -----------------------------------------------------
        // CLEAN COMMAND
        // -----------------------------------------------------

        String cleanCommand =
                command
                        .trim()
                        .toLowerCase();


        System.out.println();

        System.out.println(
                "USER COMMAND: "
                        + cleanCommand
        );


        // -----------------------------------------------------
        // REMOVE COMMON VOSK RESPONSE NOISE
        // -----------------------------------------------------

        cleanCommand =
                cleanCommand
                        .replace(
                                "yes sir",
                                ""
                        )
                        .replace(
                                "okay sir",
                                ""
                        )
                        .replace(
                                "ok sir",
                                ""
                        )
                        .trim();


        if (
                cleanCommand.isEmpty()
        ) {

            return;
        }


        System.out.println();

        System.out.println(
                "USER: "
                        + cleanCommand
        );


        // -----------------------------------------------------
        // IMPORTANT:
        // MAKE FINAL COPY BEFORE ASYNC THREAD
        // -----------------------------------------------------

        final String finalCommand =
                cleanCommand;


        // -----------------------------------------------------
        // THINKING STATE
        // -----------------------------------------------------

        processing =
                true;


        Platform.runLater(
                () -> {

                    state =
                            BuddyState.THINKING;

                    statusText =
                            "THINKING";

                    commandText =
                            finalCommand;

                    responseText =
                            "";
                }
        );


        // -----------------------------------------------------
        // ASK OLLAMA ASYNC
        // -----------------------------------------------------

        CompletableFuture.runAsync(
                () -> {

                    String answer =
                            askOllama(
                                    finalCommand
                            );


                    // -----------------------------------------
                    // UPDATE UI
                    // -----------------------------------------

                    Platform.runLater(
                            () -> {

                                responseText =
                                        answer;

                                state =
                                        BuddyState.SPEAKING;

                                statusText =
                                        "SPEAKING";

                                commandText =
                                        answer;
                            }
                    );


                    // -----------------------------------------
                    // SPEAK
                    // -----------------------------------------

                    speakAsync(
                            answer,
                            true
                    );
                }
        );
    }


    // =========================================================
    // ASK OLLAMA
    // =========================================================

    private String askOllama(
            String question
    ) {

        try {

            System.out.println();

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "          ASKING OLLAMA"
            );

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "USER: "
                            + question
            );


            // -------------------------------------------------
            // SYSTEM PROMPT
            // -------------------------------------------------

            String systemPrompt =
                    "You are Buddy, a futuristic personal voice assistant. "
                            + "You are speaking directly to your user. "
                            + "Always address the user as Sir naturally. "
                            + "Be intelligent, friendly, confident and conversational. "
                            + "Keep responses concise because the response will be spoken aloud. "
                            + "Usually answer in one or two sentences unless the user asks for detail. "
                            + "Do not use markdown. "
                            + "Do not use bullet points. "
                            + "Do not use emojis. "
                            + "Do not use special formatting. "
                            + "Do not mention that you are an AI language model. "
                            + "Do not mention Ollama unless specifically asked. "
                            + "Sound like a natural futuristic personal assistant.";


            String safeSystem =
                    escapeJson(
                            systemPrompt
                    );


            String safeQuestion =
                    escapeJson(
                            question
                    );


            // -------------------------------------------------
            // JSON
            // -------------------------------------------------

            String json =
                    "{"
                            + "\"model\":\""
                            + OLLAMA_MODEL
                            + "\","
                            + "\"stream\":false,"
                            + "\"messages\":["
                            + "{"
                            + "\"role\":\"system\","
                            + "\"content\":\""
                            + safeSystem
                            + "\""
                            + "},"
                            + "{"
                            + "\"role\":\"user\","
                            + "\"content\":\""
                            + safeQuestion
                            + "\""
                            + "}"
                            + "]"
                            + "}";


            // -------------------------------------------------
            // REQUEST
            // -------------------------------------------------

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            OLLAMA_URL
                                    )
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers
                                            .ofString(
                                                    json
                                            )
                            )
                            .build();


            // -------------------------------------------------
            // SEND
            // -------------------------------------------------

            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );


            System.out.println(
                    "Ollama HTTP Code: "
                            + response.statusCode()
            );


            // -------------------------------------------------
            // ERROR
            // -------------------------------------------------

            if (
                    response.statusCode()
                            != 200
            ) {

                System.out.println();

                System.out.println(
                        "OLLAMA ERROR:"
                );

                System.out.println(
                        response.body()
                );


                return
                        "Sorry, Sir. I couldn't reach my local AI.";
            }


            // -------------------------------------------------
            // RESPONSE
            // -------------------------------------------------

            String body =
                    response.body();


            System.out.println();

            System.out.println(
                    "OLLAMA RESPONSE:"
            );

            System.out.println(
                    body
            );


            // -------------------------------------------------
            // EXTRACT
            // -------------------------------------------------

            String answer =
                    extractOllamaContent(
                            body
                    );


            if (
                    answer == null
                            || answer.trim().isEmpty()
            ) {

                return
                        "Sorry, Sir. I couldn't think of an answer.";
            }


            // -------------------------------------------------
            // CLEAN
            // -------------------------------------------------

            answer =
                    cleanAIResponse(
                            answer
                    );


            System.out.println();

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "          BUDDY RESPONSE"
            );

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    answer
            );


            return answer;


        } catch (
                Exception e
        ) {

            System.out.println();

            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "          OLLAMA ERROR"
            );

            System.out.println(
                    "======================================"
            );


            e.printStackTrace();


            return
                    "Sorry, Sir. My local AI is not responding.";
        }
    }


    // =========================================================
    // ESCAPE JSON
    // =========================================================

    private String escapeJson(
            String text
    ) {

        if (
                text == null
        ) {

            return "";
        }


        return text
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                )
                .replace(
                        "\t",
                        "\\t"
                );
    }


    // =========================================================
    // EXTRACT OLLAMA CONTENT
    // =========================================================

    private String extractOllamaContent(
            String json
    ) {

        if (
                json == null
                        || json.isEmpty()
        ) {

            return "";
        }


        String marker =
                "\"content\":\"";


        int start =
                json.indexOf(
                        marker
                );


        if (
                start < 0
        ) {

            return "";
        }


        start +=
                marker.length();


        StringBuilder result =
                new StringBuilder();


        boolean escaped =
                false;


        for (
                int i = start;
                i < json.length();
                i++
        ) {

            char c =
                    json.charAt(i);


            if (
                    escaped
            ) {

                switch (
                        c
                ) {

                    case 'n':

                        result.append(
                                '\n'
                        );

                        break;


                    case 'r':

                        result.append(
                                '\r'
                        );

                        break;


                    case 't':

                        result.append(
                                '\t'
                        );

                        break;


                    case '"':

                        result.append(
                                '"'
                        );

                        break;


                    case '\\':

                        result.append(
                                '\\'
                        );

                        break;


                    case '/':

                        result.append(
                                '/'
                        );

                        break;


                    default:

                        result.append(
                                c
                        );

                        break;
                }


                escaped =
                        false;


                continue;
            }


            if (
                    c == '\\'
            ) {

                escaped =
                        true;

                continue;
            }


            if (
                    c == '"'
            ) {

                break;
            }


            result.append(
                    c
            );
        }


        return result.toString();
    }


    // =========================================================
    // CLEAN AI RESPONSE
    // =========================================================

    private String cleanAIResponse(
            String answer
    ) {

        if (
                answer == null
        ) {

            return "";
        }


        return answer
                .replace(
                        "**",
                        ""
                )
                .replace(
                        "*",
                        ""
                )
                .replace(
                        "#",
                        ""
                )
                .replace(
                        "`",
                        ""
                )
                .replace(
                        "\n",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }


    // =========================================================
    // TEXT TO SPEECH
    // =========================================================

    private void speakAsync(
            String text,
            boolean returnToStandby
    ) {

        if (
                text == null
                        || text.trim().isEmpty()
        ) {

            if (
                    returnToStandby
            ) {

                processing =
                        false;

                Platform.runLater(
                        this::resetToStandby
                );
            }

            return;
        }


        CompletableFuture.runAsync(
                () -> {

                    speaking =
                            true;


                    Platform.runLater(
                            () -> {

                                state =
                                        BuddyState.SPEAKING;

                                statusText =
                                        "SPEAKING";

                                commandText =
                                        text;
                            }
                    );


                    try {

                        System.out.println();

                        System.out.println(
                                "BUDDY: "
                                        + text
                        );


                        // -------------------------------------
                        // macOS Samantha voice
                        // -------------------------------------

                        ProcessBuilder builder =
                                new ProcessBuilder(
                                        "say",
                                        "-v",
                                        "Samantha",
                                        text
                                );


                        Process process =
                                builder.start();


                        process.waitFor();


                    } catch (
                            Exception e
                    ) {

                        System.out.println(
                                "TTS ERROR: "
                                        + e.getMessage()
                        );


                    } finally {

                        speaking =
                                false;


                        if (
                                returnToStandby
                        ) {

                            processing =
                                    false;


                            Platform.runLater(
                                    () -> {

                                        state =
                                                BuddyState.STANDBY;

                                        statusText =
                                                "SYSTEM STANDBY";

                                        commandText =
                                                "WAKE UP BUDDY";

                                        responseText =
                                                "";
                                    }
                            );


                            System.out.println();

                            System.out.println(
                                    "Buddy returned to standby."
                            );

                        } else {

                            /*
                             * Greeting finished.
                             *
                             * Do not immediately return to standby.
                             * The activation flow will move Buddy
                             * into LISTENING state.
                             */

                            Platform.runLater(
                                    () -> {

                                        if (
                                                state
                                                        == BuddyState.SPEAKING
                                        ) {

                                            state =
                                                    BuddyState.LISTENING;

                                            statusText =
                                                    "LISTENING";

                                            commandText =
                                                    "SPEAK YOUR COMMAND";
                                        }
                                    }
                            );
                        }
                    }
                }
        );
    }


    // =========================================================
    // TEST OLLAMA
    // =========================================================

    private void testOllama() {

        CompletableFuture.runAsync(
                () -> {

                    try {

                        HttpRequest request =
                                HttpRequest.newBuilder()
                                        .uri(
                                                URI.create(
                                                        "http://localhost:11434/api/tags"
                                                )
                                        )
                                        .GET()
                                        .build();


                        HttpResponse<String> response =
                                httpClient.send(
                                        request,
                                        HttpResponse.BodyHandlers
                                                .ofString()
                                );


                        if (
                                response.statusCode()
                                        == 200
                        ) {

                            System.out.println();

                            System.out.println(
                                    "OLLAMA: ONLINE"
                            );

                            System.out.println(
                                    "LOCAL MODEL: "
                                            + OLLAMA_MODEL
                            );

                        } else {

                            System.out.println();

                            System.out.println(
                                    "OLLAMA: HTTP "
                                            + response.statusCode()
                            );
                        }


                    } catch (
                            Exception e
                    ) {

                        System.out.println();

                        System.out.println(
                                "OLLAMA: NOT AVAILABLE"
                        );

                        System.out.println(
                                "Make sure Ollama is running."
                        );
                    }
                }
        );
    }


    // =========================================================
    // RESET
    // =========================================================

    private synchronized void resetToStandby() {

        speaking =
                false;


        processing =
                false;


        state =
                BuddyState.STANDBY;


        statusText =
                "SYSTEM STANDBY";


        commandText =
                "WAKE UP BUDDY";


        responseText =
                "";


        System.out.println();

        System.out.println(
                "Buddy reset to standby."
        );
    }


    // =========================================================
    // CREATE PARTICLES
    // =========================================================

    private void createParticles() {

        for (
                int i = 0;
                i < PARTICLE_COUNT;
                i++
        ) {

            double angle =
                    random.nextDouble()
                            * Math.PI
                            * 2.0;


            double radius =
                    70.0
                            + random.nextDouble()
                            * 620.0;


            double speed =
                    0.0003
                            + random.nextDouble()
                            * 0.001;


            double size =
                    0.5
                            + random.nextDouble()
                            * 2.5;


            particles[i] =
                    new Particle(
                            angle,
                            radius,
                            speed,
                            size
                    );
        }
    }


    // =========================================================
    // START ANIMATION
    // =========================================================

    private void startAnimation() {

        animationTimer =
                new AnimationTimer() {

                    private long lastTime =
                            0;


                    @Override
                    public void handle(
                            long now
                    ) {

                        if (
                                lastTime
                                        == 0
                        ) {

                            lastTime =
                                    now;
                        }


                        double delta =
                                (
                                        now
                                                - lastTime
                                )
                                        / 1_000_000_000.0;


                        lastTime =
                                now;


                        if (
                                delta
                                        > 0.05
                        ) {

                            delta =
                                    0.05;
                        }


                        animationTime +=
                                delta;


                        pulse +=
                                delta
                                        * 2.0;


                        updateParticles(
                                delta
                        );


                        render();
                    }
                };


        animationTimer.start();
    }


    // =========================================================
    // UPDATE PARTICLES
    // =========================================================

    private void updateParticles(
            double delta
    ) {

        double targetEnergy;


        switch (
                state
        ) {

            case STANDBY:

                targetEnergy =
                        0.25;

                break;


            case WAKE_DETECTED:

                targetEnergy =
                        1.5;

                break;


            case ACTIVATING:

                targetEnergy =
                        2.8;

                break;


            case LISTENING:

                targetEnergy =
                        2.0;

                break;


            case THINKING:

                targetEnergy =
                        3.0;

                break;


            case SPEAKING:

                targetEnergy =
                        3.5;

                break;


            default:

                targetEnergy =
                        0.5;
        }


        energy +=
                (
                        targetEnergy
                                - energy
                )
                        * delta
                        * 4.0;


        for (
                Particle p :
                particles
        ) {

            p.angle +=
                    p.speed
                            * energy
                            * 50.0
                            * delta;


            double movement =
                    Math.sin(
                            animationTime
                                    * 0.8
                                    + p.angle
                    )
                            * energy
                            * 5.0;


            p.currentRadius =
                    p.radius
                            + movement;


            // -------------------------------------------------
            // LISTENING
            // -------------------------------------------------

            if (
                    state
                            == BuddyState.LISTENING
            ) {

                p.currentRadius *=
                        0.985;
            }


            // -------------------------------------------------
            // THINKING
            // -------------------------------------------------

            if (
                    state
                            == BuddyState.THINKING
            ) {

                p.currentRadius *=
                        0.975;
            }


            // -------------------------------------------------
            // SPEAKING
            // -------------------------------------------------

            if (
                    state
                            == BuddyState.SPEAKING
            ) {

                p.currentRadius =
                        p.radius
                                + Math.sin(
                                animationTime
                                        * 4.0
                                        + p.angle
                        )
                                * 15.0;
            }
        }
    }


    // =========================================================
    // RENDER
    // =========================================================

    private void render() {

        // -----------------------------------------------------
        // BLACK BACKGROUND
        // -----------------------------------------------------

        gc.setFill(
                Color.BLACK
        );


        gc.fillRect(
                0,
                0,
                WIDTH,
                HEIGHT
        );


        double centerX =
                WIDTH / 2.0;


        double centerY =
                350;


        // -----------------------------------------------------
        // PARTICLES
        // -----------------------------------------------------

        renderParticles(
                centerX,
                centerY
        );


        // -----------------------------------------------------
        // CORE
        // -----------------------------------------------------

        renderCore(
                centerX,
                centerY
        );


        // -----------------------------------------------------
        // RINGS
        // -----------------------------------------------------

        renderRings(
                centerX,
                centerY
        );


        // -----------------------------------------------------
        // CENTRAL HUD
        // -----------------------------------------------------

        renderCentralHud(
                centerX,
                centerY
        );


        // -----------------------------------------------------
        // TEXT
        // -----------------------------------------------------

        renderText();


        // -----------------------------------------------------
        // TOP STATUS
        // -----------------------------------------------------

        renderTopStatus();


        // -----------------------------------------------------
        // BOTTOM HUD
        // -----------------------------------------------------

        renderBottomHud();
    }


    // =========================================================
    // PARTICLE RENDERING
    // =========================================================

    private void renderParticles(
            double centerX,
            double centerY
    ) {

        for (
                Particle p :
                particles
        ) {

            double x =
                    centerX
                            + Math.cos(
                            p.angle
                    )
                            * p.currentRadius;


            double y =
                    centerY
                            + Math.sin(
                            p.angle
                    )
                            * p.currentRadius
                            * 0.62;


            double alpha =
                    Math.max(
                            0.03,
                            1.0
                                    - (
                                    p.currentRadius
                                            / 720.0
                            )
                    );


            double size =
                    p.size;


            switch (
                    state
            ) {

                case LISTENING:

                    size *=
                            1.4;

                    break;


                case THINKING:

                    size *=
                            1.7;

                    break;


                case SPEAKING:

                    size *=
                            1.5
                                    + Math.sin(
                                    animationTime
                                            * 6.0
                                            + p.angle
                            )
                                    * 0.5;

                    break;


                case ACTIVATING:

                    size *=
                            1.8;

                    break;


                default:

                    break;
            }


            gc.setFill(
                    Color.rgb(
                            255,
                            145,
                            30,
                            alpha
                    )
            );


            gc.fillOval(
                    x,
                    y,
                    size,
                    size
            );
        }
    }


    // =========================================================
    // CORE
    // =========================================================

    private void renderCore(
            double x,
            double y
    ) {

        double wave =
                Math.sin(
                        animationTime
                                * 3.0
                );


        double radius =
                55.0
                        + wave
                        * 8.0
                        + energy
                        * 10.0;


        // -----------------------------------------------------
        // FUTURISTIC GLOW
        // -----------------------------------------------------

        for (
                int i = 8;
                i >= 1;
                i--
        ) {

            double r =
                    radius
                            * (
                            1.0
                                    + i
                                            * 0.16
                    );


            double alpha =
                    0.018
                            * (
                            9
                                    - i
                    );


            gc.setFill(
                    Color.rgb(
                            255,
                            120,
                            15,
                            alpha
                    )
            );


            gc.fillOval(
                    x - r,
                    y - r,
                    r * 2.0,
                    r * 2.0
            );
        }


        // -----------------------------------------------------
        // OUTER CORE
        // -----------------------------------------------------

        gc.setFill(
                Color.rgb(
                        255,
                        165,
                        45,
                        0.72
                )
        );


        gc.fillOval(
                x - radius,
                y - radius,
                radius * 2.0,
                radius * 2.0
        );


        // -----------------------------------------------------
        // INNER CORE
        // -----------------------------------------------------

        double inner =
                radius
                        * 0.55;


        gc.setFill(
                Color.rgb(
                        255,
                        205,
                        105,
                        0.90
                )
        );


        gc.fillOval(
                x - inner,
                y - inner,
                inner * 2.0,
                inner * 2.0
        );


        // -----------------------------------------------------
        // WHITE HOT CENTER
        // -----------------------------------------------------

        double center =
                radius
                        * 0.30;


        gc.setFill(
                Color.rgb(
                        255,
                        245,
                        210,
                        0.98
                )
        );


        gc.fillOval(
                x - center,
                y - center,
                center * 2.0,
                center * 2.0
        );
    }


    // =========================================================
    // RINGS
    // =========================================================

    private void renderRings(
            double x,
            double y
    ) {

        double intensity;


        switch (
                state
        ) {

            case STANDBY:

                intensity =
                        0.4;

                break;


            case WAKE_DETECTED:

                intensity =
                        1.2;

                break;


            case ACTIVATING:

                intensity =
                        1.6;

                break;


            case LISTENING:

                intensity =
                        1.8;

                break;


            case THINKING:

                intensity =
                        2.0;

                break;


            case SPEAKING:

                intensity =
                        2.2;

                break;


            default:

                intensity =
                        0.5;
        }


        // -----------------------------------------------------
        // MAIN HUD RINGS
        // -----------------------------------------------------

        for (
                int i = 0;
                i < 5;
                i++
        ) {

            double radius =
                    100.0
                            + i
                            * 55.0
                            + Math.sin(
                            animationTime
                                    * (
                                            1.0
                                                    + i
                                                            * 0.3
                                    )
                    )
                            * (
                            8.0
                                    + energy
                                    * 5.0
                    );


            gc.setStroke(
                    Color.rgb(
                            255,
                            140,
                            30,
                            0.16
                                    * intensity
                    )
            );


            gc.setLineWidth(
                    1.5
            );


            gc.strokeOval(
                    x - radius,
                    y - radius,
                    radius * 2.0,
                    radius * 2.0
            );
        }


        // -----------------------------------------------------
        // LISTENING RADAR
        // -----------------------------------------------------

        if (
                state
                        == BuddyState.LISTENING
        ) {

            renderListeningRadar(
                    x,
                    y
            );
        }


        // -----------------------------------------------------
        // THINKING ORBITS
        // -----------------------------------------------------

        if (
                state
                        == BuddyState.THINKING
        ) {

            renderThinkingOrbits(
                    x,
                    y
            );
        }


        // -----------------------------------------------------
        // SPEAKING WAVES
        // -----------------------------------------------------

        if (
                state
                        == BuddyState.SPEAKING
        ) {

            renderSpeakingWaves(
                    x,
                    y
            );
        }


        // -----------------------------------------------------
        // ACTIVATION SCANNER
        // -----------------------------------------------------

        if (
                state
                        == BuddyState.ACTIVATING
        ) {

            renderActivationScanner(
                    x,
                    y
            );
        }
    }


    // =========================================================
    // LISTENING RADAR
    // =========================================================

    private void renderListeningRadar(
            double x,
            double y
    ) {

        for (
                int i = 0;
                i < 48;
                i++
        ) {

            double angle =
                    i
                            * Math.PI
                            * 2.0
                            / 48.0;


            double wave =
                    Math.sin(
                            animationTime
                                    * 5.0
                                    + i
                                    * 0.65
                    );


            double radius =
                    190.0
                            + wave
                            * 32.0;


            double x1 =
                    x
                            + Math.cos(
                            angle
                    )
                            * radius;


            double y1 =
                    y
                            + Math.sin(
                            angle
                    )
                            * radius;


            double length =
                    10.0
                            + Math.abs(
                            wave
                    )
                            * 25.0;


            double x2 =
                    x
                            + Math.cos(
                            angle
                    )
                            * (
                            radius
                                    + length
                    );


            double y2 =
                    y
                            + Math.sin(
                            angle
                    )
                            * (
                            radius
                                    + length
                    );


            gc.setStroke(
                    Color.rgb(
                            255,
                            180,
                            70,
                            0.75
                    )
            );


            gc.setLineWidth(
                    1.5
            );


            gc.strokeLine(
                    x1,
                    y1,
                    x2,
                    y2
            );
        }
    }


    // =========================================================
    // THINKING ORBITS
    // =========================================================

    private void renderThinkingOrbits(
            double x,
            double y
    ) {

        for (
                int i = 0;
                i < 4;
                i++
        ) {

            double angle =
                    animationTime
                            * (
                            1.3
                                    + i
                                    * 0.35
                    );


            double radius =
                    220.0
                            + i
                            * 38.0;


            double px =
                    x
                            + Math.cos(
                            angle
                    )
                            * radius;


            double py =
                    y
                            + Math.sin(
                            angle
                    )
                            * radius
                            * 0.55;


            gc.setFill(
                    Color.rgb(
                            255,
                            200,
                            100,
                            0.95
                    )
            );


            gc.fillOval(
                    px - 5.0,
                    py - 5.0,
                    10.0,
                    10.0
            );


            gc.setStroke(
                    Color.rgb(
                            255,
                            150,
                            40,
                            0.35
                    )
            );


            gc.strokeOval(
                    x - radius,
                    y - radius * 0.55,
                    radius * 2.0,
                    radius * 1.1
            );
        }
    }


    // =========================================================
    // SPEAKING WAVES
    // =========================================================

    private void renderSpeakingWaves(
            double x,
            double y
    ) {

        for (
                int i = 0;
                i < 5;
                i++
        ) {

            double radius =
                    150.0
                            + i
                            * 35.0
                            + (
                            Math.sin(
                                    animationTime
                                            * 5.0
                                            - i
                            )
                                    * 18.0
                    );


            gc.setStroke(
                    Color.rgb(
                            255,
                            160,
                            50,
                            0.35
                    )
            );


            gc.setLineWidth(
                    2.0
            );


            gc.strokeOval(
                    x - radius,
                    y - radius,
                    radius * 2.0,
                    radius * 2.0
            );
        }
    }


    // =========================================================
    // ACTIVATION SCANNER
    // =========================================================

    private void renderActivationScanner(
            double x,
            double y
    ) {

        double scan =
                (
                        Math.sin(
                                animationTime
                                        * 5.0
                        )
                                + 1.0
                )
                        * 0.5;


        double radius =
                120.0
                        + scan
                        * 170.0;


        gc.setStroke(
                Color.rgb(
                        255,
                        190,
                        80,
                        0.8
                )
        );


        gc.setLineWidth(
                3.0
        );


        gc.strokeOval(
                x - radius,
                y - radius,
                radius * 2.0,
                radius * 2.0
        );
    }


    // =========================================================
    // CENTRAL HUD
    // =========================================================

    private void renderCentralHud(
            double x,
            double y
    ) {

        gc.setStroke(
                Color.rgb(
                        255,
                        160,
                        50,
                        0.35
                )
        );


        gc.setLineWidth(
                1.0
        );


        // -----------------------------------------------------
        // CROSSHAIR
        // -----------------------------------------------------

        gc.strokeLine(
                x - 320,
                y,
                x - 250,
                y
        );


        gc.strokeLine(
                x + 250,
                y,
                x + 320,
                y
        );


        gc.strokeLine(
                x,
                y - 250,
                x,
                y - 190
        );


        gc.strokeLine(
                x,
                y + 190,
                x,
                y + 250
        );


        // -----------------------------------------------------
        // SMALL TECH MARKERS
        // -----------------------------------------------------

        gc.setFill(
                Color.rgb(
                        255,
                        170,
                        60,
                        0.7
                )
        );


        for (
                int i = 0;
                i < 12;
                i++
        ) {

            double angle =
                    animationTime
                            * 0.25
                            + i
                            * Math.PI
                            * 2.0
                            / 12.0;


            double radius =
                    310.0;


            double px =
                    x
                            + Math.cos(
                            angle
                    )
                            * radius;


            double py =
                    y
                            + Math.sin(
                            angle
                    )
                            * radius
                            * 0.62;


            gc.fillRect(
                    px - 2,
                    py - 2,
                    4,
                    4
            );
        }
    }


    // =========================================================
    // MAIN TEXT
    // =========================================================

    private void renderText() {

        double centerX =
                WIDTH / 2.0;


        // -----------------------------------------------------
        // BUDDY TITLE
        // -----------------------------------------------------

        gc.setFill(
                Color.rgb(
                        255,
                        155,
                        45,
                        0.98
                )
        );


        gc.setFont(
                Font.font(
                        "Arial",
                        FontWeight.BOLD,
                        42
                )
        );


        String title =
                "B U D D Y";


        double titleWidth =
                measureText(
                        title,
                        gc.getFont()
                );


        gc.fillText(
                title,
                centerX
                        - titleWidth / 2.0,
                670
        );


        // -----------------------------------------------------
        // STATUS
        // -----------------------------------------------------

        gc.setFont(
                Font.font(
                        "Arial",
                        FontWeight.NORMAL,
                        18
                )
        );


        double statusWidth =
                measureText(
                        statusText,
                        gc.getFont()
                );


        gc.setFill(
                Color.rgb(
                        255,
                        185,
                        80,
                        0.95
                )
        );


        gc.fillText(
                statusText,
                centerX
                        - statusWidth / 2.0,
                705
        );


        // -----------------------------------------------------
        // COMMAND / RESPONSE
        // -----------------------------------------------------

        gc.setFont(
                Font.font(
                        "Arial",
                        FontWeight.NORMAL,
                        15
                )
        );


        String displayText =
                commandText;


        if (
                displayText == null
        ) {

            displayText =
                    "";
        }


        // -----------------------------------------------------
        // LIMIT LONG TEXT
        // -----------------------------------------------------

        if (
                displayText.length()
                        > 100
        ) {

            displayText =
                    displayText.substring(
                            0,
                            97
                    )
                            + "...";
        }


        double commandWidth =
                measureText(
                        displayText,
                        gc.getFont()
                );


        gc.setFill(
                Color.rgb(
                        220,
                        220,
                        220,
                        0.85
                )
        );


        gc.fillText(
                displayText,
                centerX
                        - commandWidth / 2.0,
                735
        );
    }


    // =========================================================
    // TOP STATUS
    // =========================================================

    private void renderTopStatus() {

        gc.setFont(
                Font.font(
                        "Arial",
                        FontWeight.NORMAL,
                        13
                )
        );


        gc.setFill(
                Color.rgb(
                        255,
                        160,
                        50,
                        0.85
                )
        );


        gc.fillText(
                "BUDDY AI // LOCAL INTELLIGENCE CORE",
                35,
                40
        );


        gc.setFill(
                Color.rgb(
                        180,
                        180,
                        180,
                        0.65
                )
        );


        gc.fillText(
                "OLLAMA // LLAMA 3.2",
                35,
                62
        );


        // -----------------------------------------------------
        // ONLINE DOT
        // -----------------------------------------------------

        double onlinePulse =
                0.7
                        + Math.sin(
                        animationTime
                                * 3.0
                )
                                * 0.25;


        gc.setFill(
                Color.rgb(
                        255,
                        160,
                        50,
                        onlinePulse
                )
        );


        gc.fillOval(
                WIDTH - 65,
                31,
                10,
                10
        );


        gc.setFill(
                Color.rgb(
                        200,
                        200,
                        200,
                        0.7
                )
        );


        gc.fillText(
                "ONLINE",
                WIDTH - 110,
                41
        );
    }


    // =========================================================
    // BOTTOM HUD
    // =========================================================

    private void renderBottomHud() {

        gc.setFont(
                Font.font(
                        "Arial",
                        FontWeight.NORMAL,
                        11
                )
        );


        gc.setFill(
                Color.rgb(
                        255,
                        150,
                        50,
                        0.55
                )
        );


        gc.fillText(
                "VOICE CORE",
                35,
                HEIGHT - 35
        );


        gc.fillText(
                "VOSK STT",
                35,
                HEIGHT - 18
        );


        gc.fillText(
                "WAKE WORD: ACTIVE",
                WIDTH - 165,
                HEIGHT - 35
        );


        gc.fillText(
                "LOCAL AI",
                WIDTH - 165,
                HEIGHT - 18
        );
    }


    // =========================================================
    // TEXT MEASUREMENT
    // =========================================================

    private double measureText(
            String text,
            Font font
    ) {

        if (
                text == null
        ) {

            return 0;
        }


        return text.length()
                * font.getSize()
                * 0.55;
    }


    // =========================================================
    // STOP
    // =========================================================

    @Override
    public void stop() {

        System.out.println();

        System.out.println(
                "Stopping Buddy..."
        );


        speaking =
                false;


        processing =
                false;


        if (
                voiceService != null
        ) {

            try {

                voiceService.stopListening();

            } catch (
                    Exception e
            ) {

                System.out.println(
                        "Voice shutdown error: "
                                + e.getMessage()
                );
            }
        }


        if (
                animationTimer != null
        ) {

            animationTimer.stop();
        }


        System.out.println(
                "Buddy stopped."
        );
    }


    // =========================================================
    // PARTICLE CLASS
    // =========================================================

    private static class Particle {

        double angle;

        double radius;

        double currentRadius;

        double speed;

        double size;


        Particle(
                double angle,
                double radius,
                double speed,
                double size
        ) {

            this.angle =
                    angle;

            this.radius =
                    radius;

            this.currentRadius =
                    radius;

            this.speed =
                    speed;

            this.size =
                    size;
        }
    }


    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        launch(
                args
        );
    }
}