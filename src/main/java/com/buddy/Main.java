package com.buddy;

public class Main {

    public static void main(String[] args) {

        System.out.println();
        System.out.println("========================================");
        System.out.println("             BUDDY AI");
        System.out.println("========================================");

        System.out.println();
        System.out.println("SYSTEM       : INITIALIZING");
        VoiceService voice = new VoiceService();

voice.startListening();
        System.out.println("AI CORE      : OFFLINE");
        System.out.println("VOICE        : OFFLINE");
        System.out.println("RAG          : OFFLINE");
        System.out.println("MEMORY       : OFFLINE");
        System.out.println("HUD          : OFFLINE");

        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println("             BUDDY STANDBY");
        System.out.println("----------------------------------------");
        System.out.println();

    }
}