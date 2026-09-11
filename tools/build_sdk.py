#!/usr/bin/env python3
"""Build ThreadTabs v2 with Gradle, including its Google authorization library."""
import os,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
wrapper=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
subprocess.run([str(wrapper),'assembleDebug'],cwd=root,check=True)
print(root/'app/build/outputs/apk/debug/app-debug.apk')
