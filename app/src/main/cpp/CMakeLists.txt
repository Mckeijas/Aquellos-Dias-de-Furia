# Logros de RetroAchievements: rcheevos (licencia MIT, ver rcheevos/LICENSE) + puente JNI.
cmake_minimum_required(VERSION 3.22.1)
project(logros C)

file(GLOB RC_FUENTES
    rcheevos/src/api/*.c
    rcheevos/src/runtime/*.c
    rcheevos/src/util/*.c
    rcheevos/src/client/rc_client.c
)

add_library(logros SHARED logros.c audio_red.c ${RC_FUENTES})
target_include_directories(logros PRIVATE rcheevos/include rcheevos/src)
target_compile_definitions(logros PRIVATE RC_DISABLE_LUA)
target_compile_options(logros PRIVATE -O2 -w)
target_link_libraries(logros log dl m)
