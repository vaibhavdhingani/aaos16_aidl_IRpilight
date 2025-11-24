#include <utils/Log.h>
#include <iostream>
#include <fstream>
#include "Rpilight.h"

namespace aidl {
namespace android {
namespace hardware {
namespace rpilight {

ndk::ScopedAStatus Rpilight::ledControl(int32_t in_state, int32_t* _aidl_return) {
    constexpr char LED_FILE_PATH[] = "/sys/class/leds/ACT/brightness";
    constexpr char LED_ON_VALUE[] = "255";
    constexpr char LED_OFF_VALUE[] = "0";

    int fd = open(LED_FILE_PATH, O_WRONLY);
    if (fd < 0) {
        ALOGE("Failed to open %s", LED_FILE_PATH);
        return ndk::ScopedAStatus::fromServiceSpecificError(-1);
    }

    int ret = 0;
    if (in_state > 0) {
        ret = write(fd, LED_ON_VALUE, sizeof(LED_ON_VALUE) - 1); // Write "255" (LED ON)
        ALOGE("Setting LED On  %s", LED_ON_VALUE);
    } else {
        ret = write(fd, LED_OFF_VALUE, sizeof(LED_OFF_VALUE) - 1); // Write "0" (LED OFF)
        ALOGE("Setting LED Off  %s", LED_OFF_VALUE);
    }

    close(fd);

    if (ret < 0) {
        ALOGE("Failed to write to %s", LED_FILE_PATH);
        return ndk::ScopedAStatus::fromServiceSpecificError(-1);
    }

    *_aidl_return = true;
    return ndk::ScopedAStatus::ok();
}

}  // namespace rpilight
}  // namespace hardware
}  // namespace android
}
