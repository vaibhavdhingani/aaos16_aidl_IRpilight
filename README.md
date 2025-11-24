# AAOS 16: Building a Stable AIDL HAL (IRpilight) for Raspberry Pi 4
***HAL** is the **bridge between the Android Framework and hardware implementations. HAL (Hardware Abstraction Layer)** in AOSP is a **layer of code** that lets Android Framework interact with **device hardware or low-level system components** (like sensors, camera, radio, or even software-only subsystems) without knowing the details of the hardware implementation, enabling seamless communication while maintaining modularity and adaptability. Understanding **Android HAL development** is crucial for optimizing hardware interactions, enhancing performance, and ensuring compatibility across different devices.*

***AIDL (Android Interface Definition Language)** for a Custom Hardware Abstraction Layer (HAL) in Android allows OEM to adjust interactions with the hardware according to the peculiarities of an application. With the assistance of AIDL, the OEM have the possibility to create distinct links regarding the Android framework and device hardware, which eliminates the possibility of conflict and contributes to more efficient functioning. Besides, this approach contributes to the improvement of the flexibility of further development and the formation of the sets of optimized solutions regarding Android gadgets.*

For those unfamiliar, **Android Open-Source Platform (AOSP)** is built on the Linux kernel and other open-source components, each functioning as a module to create specific layers of functionality. This modular structure allows for extensive customization, enabling device manufacturers and app developers to integrate unique features and functionalities. Over time, the Android architecture has undergone significant changes and continues to evolve with contributions from companies and open-source developers.*

### 🔍 Why HAL Exists
*Android runs on a wide variety of devices. Instead of writing custom framework code for every hardware, HAL allows:*
- **🔁 Standardized Interfaces ↔ Custom Implementations** 
- The **Android Framework** talks to a **standard API**
- The **Vendor/OEM** implements this API using real hardware drivers or emulation

### Adding a Custom AIDL HAL to the Android Stack
To integrate a new custom AIDL HAL into the Android stack, it is crucial to comprehend the AOSP layers and the way a custom HAL may correspond with the rest of the stack. Understanding the **hardware abstraction layer in Android** is essential, as it acts as a bridge between the device hardware and the higher-level system components, ensuring smooth communication and modular implementation. The next diagram represents the AOSP stack, where each layer is presented, as well as the moment of integration of the tested custom HAL along with the existing ones.  
<img width="587" height="864" alt="aaos_aidl_arch" src="https://github.com/user-attachments/assets/121905d5-c85d-42dd-a410-63fe9f7d7cf3" />


### GOAL
Build a minimal **AIDL-based HAL** named IRpilight.aidl to toggle Raspberry PI 4 on board LED with:
- A single method: int ledControl(in int state);
- Full HAL skeleton:
- AIDL file
- C++ service implementation
- Init .rc file
- Android.bp files
- Device manifest registration
- Execute on AAOS16 running on Raspberry pi 4

### Adopting Stable AIDL in HAL Implementations
*Implementations of HAL in Android are found in the hardware/interface directory while the AIDL generated HAL interfaces are found in the aidl directory. For instance, the rpilight AIDL HAL is at hardware/interfaces/rpilight; the following shows the directory structure of the files.*  
<img width="446" height="507" alt="rpilight_folder_structure" src="https://github.com/user-attachments/assets/ff0e3af8-4f4e-4543-8028-b1335878ec38" />

HALs that use AIDL to communicate between framework components (for example, System. img) and hardware components (for example, vendor. img) must use Stable AIDL, It can be observed where there are interactions between system and vendor partition, these use Stable AIDL. The following changes are needed to make the use of an AIDL interface between the system and the vendor possible.  


### 🧩 1. Define AIDL Interface
📄 hardware/interfaces/rpilight/aidl/android/hardware/rpilight/c.aidl
```
package android.hardware.rpilight;                                                                                                           
@VintfStability
interface IRpilight {
  int ledControl(in int state);
}
```
**VintfStability** is an annotation used in AIDL to mark the stability level of the interface. It’s required for HALs that are used in the Android Vendor Interface (VINTF) and must follow stability guarantees between the Android framework and vendor.


#### 🔍 What is VINTF?
**VINTF (Vendor Interface)** ensures that:
- The **framework and vendor** parts of the OS remain **compatible**, even if updated separately.
- The HAL interface must be **versioned and stable.**

### 🧱 2. AIDL Android.bp

📄 hardware/interfaces/myhal/aidl/Android.bp
```
package {
    default_applicable_licenses: ["hardware_interfaces_license"],
}

aidl_interface {                                                                                                                               
    name: "android.hardware.rpilight",
    vendor_available: true,
    srcs: ["android/hardware/rpilight/*.aidl"],
    stability: "vintf",
    owner: "vaibhav dhingani",
    backend: {
        cpp: {
            enabled: false,
        },
        java: {
            sdk_version: "module_current",
        },
        ndk: {
            enabled: true,
        },
    },  
    versions_with_info: [
        {
            version: "1",
            imports: [], 
        },
    ],  
    frozen: true,
}
```

#### ⚙️ What the Build System Will Generate:
After you define IRpilight.aidl and run following commands.

```
$ m android.hardware.rpilight-update-api
$ m android.hardware.rpilight-freeze-api
$ mmm hardware/interfaces/rpilight/
```

The system generates:
- BnRpilight.h (C++ server-side base class)
- BpRpilight.h (C++ client-side proxy)
- I*Hal.h interfaces
- Stub and proxy binder code

Location: under 📄 out/soong/.intermediates/android.hardware.rpilight/.../gen/include/aidl/android/hardware/rpilight


### 🧠 3. C++ Implementation
📄 Rpilight.h 

```
#pragma once                                                                                                                #include <aidl/android/hardware/rpilight/BnRpilight.h>
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
```

📄 MyHal.cpp

```
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
```

📄 service.cpp
```
define LOG_TAG "Rpilight"
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
```


### 🔧 4. Init RC File
📄 android.hardware.rpilight-service.rc

```
service android.hardware.rpilight-service /vendor/bin/hw/android.hardware.rpilight-service                                                     
        interface aidl android.hardware.rpilight.IRpilight/default
        class hal 
        user system
        group system
```

**service rpilight:** Declares a service named rpilight.
**/vendor/bin/hw/android.hardware.rpilight-service:** The executable that will be launched when this service starts.
- This binary is typically the AIDL HAL implementation binary, compiled via the defaultServiceImplementation = true directive in Android.bp.

**class hal**
Assigns this service to the hal class.
- The hal class is triggered during early boot, typically during init, so your HAL starts early in the boot process (before system services).  
**user system**
**group system**
Specifies that the service should run as the system user and group.
- This determines what permissions the service will have.
- For system user/group is common for HALs because it has sufficient privileges to access devices or Binder.


### 🧰 5. HAL Service Build File
📄 default/Android.bp

```
package {                                                                                                                                      
    default_applicable_licenses: ["hardware_interfaces_license"],
}

cc_binary {
    name: "android.hardware.rpilight-service",
    vendor: true,
    relative_install_path: "hw",
    init_rc: ["android.hardware.rpilight-service.rc"],
    vintf_fragments: ["android.hardware.rpilight-service.xml"],
    srcs: [
        "service.cpp",
        "Rpilight.cpp",
    ],  
    cflags: [
        "-Wall",
        "-Werror",
    ],  
    aidl: {
        include_dirs: ["hardware/interfaces/rpilight/aidl"],
    },  
    shared_libs: [
        "android.hardware.rpilight-V1-ndk",
        "libbase",
        "liblog",
        "libhardware",
        "libbinder_ndk",
        "libbinder",
        "libutils",
    ],  
}

prebuilt_etc {
    name: "android.hardware.rpilight-service.xml",
    src: "android.hardware.rpilight-service.xml",
    sub_dir: "vintf",
    installable: false,
}

filegroup {
    name: "android.hardware.rpilight-service.rc",
    srcs: ["android.hardware.rpilight-service.rc"],
} 
```


### 📦 6. Device manifest.xml
📄 default/android.hardware.rpilight-service.xml
```
<manifest version="1.0" type="device">                                                                                                         
    <hal format="aidl">
        <name>android.hardware.rpilight</name>
        <version>1</version>
        <interface>
            <name>IRpilight</name>
            <instance>default</instance>
        </interface>
    </hal>
</manifest>
```

### 🔨 7. Build & Deploy
Add application package in device make file present at **device/brcm/rpi4/device.mk**
```
# Rpilight
PRODUCT_PACKAGES += \
   android.hardware.rpilight-service
```
**Build HAL**
```
$ m android.hardware.rpilight-service
```
**Push and run manually (for emulator/testing)**
```
$ adb root
$ adb remount
$ adb push out/target/product/rpi4/vendor/bin/hw/android.hardware.rpilight-service /vendor/bin/
$ adb shell /vendor/bin/hw/android.hardware.rpilight-service
```


### ✅ 8. Verify
```
$ adb shell lshal | grep rpilight
$ adb shell service list | grep rpilight
```
Java Test App to Call **Rpilight::ledControl(in int state)**.
You can write a Java test client that connects to your HAL using IBinder and IRpilight.Stub.asInterface().



## 📱 Java Test App -  Developing an Android Application: RpiLEDControl
The Android application serves as the user interface. Using the RpiLEDControl application, users can toggle the Raspberry pi onboard  LED on and off. To integrate this application into the AOSP tree, it should be located at packages/apps/RpiLEDControl.

**RpiLEDControl App Structure**  
In the RpiLEDControl app:
- MainActivity.java: Implement functionality to control the LED.
- res directory: Add necessary resources to create a custom user interface.  
<img width="343" height="304" alt="rpilight_app_folder_structure" src="https://github.com/user-attachments/assets/b07c0aaf-efad-4684-a50e-ec5115ea090e" />


**Accessing System Service Interface**  
To access the rpilight service method from the frameworks:
- Obtain the system service interface to facilitate communication with the rpilight service

📄 MainActivity.java
```
public class MainActivity extends Activity {
    private ServiceConnection serviceConnection;
    private static final String TAG = "RpiLEDControl"; 
    private IRpilight mRpilightService;
    IBinder mRpilightBinder;
    public final String PACKAGE_CLUSTERHAL = "android.hardware.rpilight.IRpilight/default";

    private Button ledTurnOn, ledTurnOff;
    private TextView stateTextView;
    boolean isLEDOn = true;

@Override
protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Starting rpilight_apk");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ledTurnOn = (Button)findViewById(R.id.ledTurnOn);
        ledTurnOff = (Button)findViewById(R.id.ledTurnOff);
        stateTextView = (TextView)findViewById(R.id.stateTextView);

        Log.i(TAG, "ledTurnOn ledTurnOff Created");
        Log.i(TAG, "Started");
        mRpilightBinder = ServiceManager.waitForDeclaredService(PACKAGE_CLUSTERHAL);
        Log.i(TAG, "Came out from ServiceManager.waitForDeclaredService");
        mRpilightService = IRpilight.Stub.asInterface(mRpilightBinder);
        Log.i(TAG, "Interface created");

        try {
            int state = isLEDOn ? 0 : 1;
            Log.i(TAG, "RpiLight : Setting LED to: " + state);
            int result = mRpilightService.ledControl(state);
            Log.i(TAG, "LED control result: " + result);
            isLEDOn = !isLEDOn;
        } catch (RemoteException e) {
            e.printStackTrace();
        }


ledTurnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "ledTurnOn Clicked");
                mRpilightBinder = ServiceManager.waitForDeclaredService(PACKAGE_CLUSTERHAL);
                Log.i(TAG, "Came out from ServiceManager.waitForDeclaredService");
                mRpilightService = IRpilight.Stub.asInterface(mRpilightBinder);
                Log.i(TAG, "Interface created");

                try {
                    int state = 1;
                    Log.i(TAG, "RpiLight : Setting LED to: " + state);
                    int result = mRpilightService.ledControl(state);
                    Log.i(TAG, "LED control result: " + result);
                    isLEDOn = !isLEDOn;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }                                                                                      
                Toast.makeText(MainActivity.this, "LED Turn On", Toast.LENGTH_SHORT).show();
                stateTextView.setText("LED is ON");

            }
        });

ledTurnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "ledTurnOff Clicked");
                mRpilightBinder = ServiceManager.waitForDeclaredService(PACKAGE_CLUSTERHAL);
                Log.i(TAG, "Came out from ServiceManager.waitForDeclaredService");
                mRpilightService = IRpilight.Stub.asInterface(mRpilightBinder);
                Log.i(TAG, "Interface created");

                try {
                    int state = 0;
                    Log.i(TAG, "RpiLight : Setting LED to: " + state);
                    int result = mRpilightService.ledControl(state);
                    Log.i(TAG, "LED control result: " + result);
                    isLEDOn = !isLEDOn;

                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                Toast.makeText(MainActivity.this, "LED Turn OFF", Toast.LENGTH_SHORT).show();
                stateTextView.setText("LED is OFF");
            }
        });
    }

@Override
protected void onStop() {
        super.onStop();
        unbindService(serviceConnection);
    }

private void updateStateText(boolean isChecked) {
        stateTextView.setText("LED State : ");
    }
}                                                

```

Add application package in device make file present at **device/brcm/rpi4/device.mk**
```
PRODUCT_PACKAGES += \
         RpiLEDControl
```

## Conclusion
*Implementing an own AIDL HAL into the Android stack expands the agendas of the platform and gives more options for individualization. Thus, the use of AIDL enables the control of a device’s hardware to be built with concrete considerations in mind and thus become intertwined with the rest of the Android environment. This enhances the stability of the system and encourages the growth of new innovations since the applications are in a position to communicate directly with the hardware. The use of AIDL HALs in Android also suggests the OS’s consistent growth in offering cutting-edge, and highly elastic solutions to technological challenges.*
