#define LOG_TAG "Rpilight"

#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include "Rpilight.h"

using aidl::android::hardware::rpilight::Rpilight;
using std::string_literals::operator""s;

void loge(std::string msg) {
    std::cout << msg << std::endl;
    ALOGE("%s", msg.c_str());
}

int main() {

    ALOGI("Rpilight service is starting");

    android::ProcessState::initWithDriver("/dev/vndbinder");
    ABinderProcess_setThreadPoolMaxThreadCount(2);
    ABinderProcess_startThreadPool();

    std::shared_ptr<Rpilight> rpilight = ndk::SharedRefBase::make<Rpilight>();
    const std::string instance = std::string() + Rpilight::descriptor + "/default";

    if (rpilight != nullptr) {
        if(AServiceManager_addService(rpilight->asBinder().get(), instance.c_str()) != STATUS_OK) {
        
            loge(instance.c_str());
            loge("Failed to register Rpilight service");
            return -1;
        }
    } else {
        loge("Failed to get IRpilight instance");
        return -1;
    }

    loge("Rpilight service starts to join service pool");
    ABinderProcess_joinThreadPool();

    return EXIT_FAILURE;  // should not reached
}
