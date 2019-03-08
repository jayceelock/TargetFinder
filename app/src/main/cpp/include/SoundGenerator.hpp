#ifndef SOUND_GENERATOR
#define SOUND_GENERATOR

#define SOUNDLOG "SoundGenerator.cpp"
#define NUM_BUFFERS 1
#define SOUND_LEN 8
#define SAMPLE_RATE 44100
#define NUM_SEMITONES 120

#include <jni.h>
#include <malloc.h>
#include <cmath>
#include <cstdlib>
#include <cstring>

#include <android/log.h>

#include <AL/al.h>
#include <AL/alc.h>
#include <AL/alext.h>

namespace SoundGeneratorSpace
{
    class SoundGenerator
    {
    public:
        SoundGenerator();
        ~SoundGenerator();

        // Initialise and kill OpenAL library
        bool init();
        bool kill();

        bool startSound();
        bool endSound();

        // Sound generating functions
        short* generateSoundWave(size_t bufferSize, jfloat pitch, short lastVal, bool onUpSwing);
        short convertToneToSemitone(float pitch);
        void play(JNIEnv* env, jfloatArray src, jfloatArray list, jfloat gain, jfloat pitch);
        void playOnTarget(JNIEnv* env, jfloat gain, jfloat pitch);
        void startPlay(ALuint src, ALuint *buf, jfloat pitch);
        void updatePlay(jfloat pitch);

        // Helper functions
        bool isSourcePlaying(ALuint src);

    private:
        ALuint soundSrc;
        ALuint soundBuf[NUM_BUFFERS];

        ALuint onTargetSrc;
        ALuint onTargetBuf[NUM_BUFFERS];

        // bool onTargetPlaying = false;
        // bool soundPlaying = false;

        float notes[NUM_SEMITONES];
    };
}

#endif