#pragma once
#include <aidl/android/hardware/rpilight/BnRpilight.h>

namespace aidl {
  namespace android {
    namespace hardware {
      namespace rpilight {
        class Rpilight: public BnRpilight {
        public:
            ndk::ScopedAStatus ledControl(int32_t in_state, int32_t* _aidl_return) override;
        };
    }
    }
    }
}
