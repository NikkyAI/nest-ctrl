package nestdrop.deck

enum class Effect {
    AnimationSpeed,
    ZoomSpeed,
    RotationSpeed,
    WrapSpeed,
    HorizonMotion,
    VerticalMotion,
    RandomMotion,
    StretchSpeed,
    WaveMode,
    SolidColor,
    Negative,
    Brightness,
    Contrast,
    Gamma,
    Hue,
    Saturation,
    LumaKeyMin,
    LumaKeyMax,
    Red,
    Green,
    Blue,
    Alpha,
}

enum class Trigger {
    TimesPerSecond,
    BPM,
    VolumePeak,
    BassPeak,
    MidPeak,
    TreblePeak
}

enum class Waveform {
    Square,
    Sawtooth,
    Sine,
}