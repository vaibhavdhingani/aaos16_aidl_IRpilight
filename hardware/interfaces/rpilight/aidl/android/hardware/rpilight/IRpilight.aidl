package android.hardware.rpilight;
@VintfStability
interface IRpilight {
  int ledControl(in int state);
}
