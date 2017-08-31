.PHONY: build
build:
	./gradlew assembleDebug

.PHONY: release
release:
	@stty -echo && read -p "Key password: " pwd && stty echo && \
	STORE_PASSWORD=$$pwd KEY_PASSWORD=$$pwd ./gradlew assembleRelease

.PHONY: clean
clean:
	./gradlew clean
