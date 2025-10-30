name: Build APK

on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Fix line endings
      run: |
        sed -i 's/\r$//' gradlew
        chmod +x gradlew

    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Build APK
      run: ./gradlew assembleDebug

    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: ShareToComputer-APK
        path: app/build/outputs/apk/debug/app-debug.apk
        retention-days: 30
