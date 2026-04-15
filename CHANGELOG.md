## [3.3.0](https://github.com/giovihey/UFG/compare/v3.2.2...v3.3.0) (2026-04-15)

### Features

* recovery of the changes done before WebRTC bridge ([cbc8326](https://github.com/giovihey/UFG/commit/cbc8326a0f27d7133b9db4fc5472545e93a99eac))

### Bug Fixes

* merge multiplayer ([e36970c](https://github.com/giovihey/UFG/commit/e36970ca3e8093fa45bb753f62794f9d67a00c73))
* now build on Windows ([5ffa51f](https://github.com/giovihey/UFG/commit/5ffa51ff18a9c97751244d3dd54955454ed01872))
* now build on Windows ([063e275](https://github.com/giovihey/UFG/commit/063e2758a3edc0b931ba2f052c914d6984c74c0d))
* now make works Windows ([8a6242a](https://github.com/giovihey/UFG/commit/8a6242a8b2e39cc20a7357398dcd1f92df1202a3))

## [3.2.2](https://github.com/giovihey/UFG/compare/v3.2.1...v3.2.2) (2026-04-13)

### Bug Fixes

* graceful shutdown from libdatachannel tunnel ([97c475f](https://github.com/giovihey/UFG/commit/97c475f295c154e6c4738df4f6cd67c2eb0004e1))
* implement graceful shutdown to remove deadlock on closing ([9d6bb6f](https://github.com/giovihey/UFG/commit/9d6bb6f21ff3df9c0b525af27352fcb274f7ad51))
* remove meta file ([c579af9](https://github.com/giovihey/UFG/commit/c579af9c1a18541728535210a42d71f04d536859))
* update blocking logic, but it should still fail ([872022c](https://github.com/giovihey/UFG/commit/872022c202d639dd11fd7370c38804ca79198d7d))

## [3.2.1](https://github.com/giovihey/UFG/compare/v3.2.0...v3.2.1) (2026-04-10)

### Bug Fixes

* remove meta file ([bfb74da](https://github.com/giovihey/UFG/commit/bfb74da40ee0a3705311655628e8ac8a2103eb8a))

## [3.2.0](https://github.com/giovihey/UFG/compare/v3.1.0...v3.2.0) (2026-03-30)

### Features

* first working implementation ([5166355](https://github.com/giovihey/UFG/commit/516635522b81947736062459456958c311555f15))
* passing to multiplayer ([6bf90e1](https://github.com/giovihey/UFG/commit/6bf90e1f838a44dec3e637e6ba24af2955fdcd8e))

### Bug Fixes

* build and wertc wrapper imports ([f3b5321](https://github.com/giovihey/UFG/commit/f3b5321fbefdba1ff0139ca07b616e4e60d4bb32))
* change ci to control all the components ([c0330db](https://github.com/giovihey/UFG/commit/c0330db8d114ca4a619f3de745d3b0ce648ca70e))
* cmake typo in tests ([a0e0149](https://github.com/giovihey/UFG/commit/a0e0149d74ce028244dbe1eaa7756f9ed814f71a))
* typos and broken tests ([692dd0c](https://github.com/giovihey/UFG/commit/692dd0cd49be9364906649db1591af55d67fa1f2))

## [3.1.0](https://github.com/giovihey/UFG/compare/v3.0.0...v3.1.0) (2026-03-26)

### Features

* add a simple app demo ([3443478](https://github.com/giovihey/UFG/commit/34434784a096842c3061a047bd9de1cfe04bfb04))
* add HitDetectionSystem for future implementation ([3163b9c](https://github.com/giovihey/UFG/commit/3163b9c7eeb0239e2921a5ffd620a6f730ebdc70))
* add initial GameEngine and GameLoop ([2eec844](https://github.com/giovihey/UFG/commit/2eec844fbed349b996e5ffcfc4e0d3066a00f074))
* add jump and started testing the input ([c78496e](https://github.com/giovihey/UFG/commit/c78496e9e48e052cb0603fb53571ad9615e41952))
* add physics system for player ([66548e1](https://github.com/giovihey/UFG/commit/66548e18f44de186faf0a298ed61677de025fdc6))
* add player state ([083f8ce](https://github.com/giovihey/UFG/commit/083f8ced430f9288efbb77e672d95a53a7367e49))
* add some test for physics system ([45a257d](https://github.com/giovihey/UFG/commit/45a257d4d41d00713f67825fef25099d73ac8883))
* change structure for ECS ([1e4215f](https://github.com/giovihey/UFG/commit/1e4215f7563c51d044d8614ff68ab9daebac8be9))
* changing better domain structure ([8c1ef3d](https://github.com/giovihey/UFG/commit/8c1ef3d941a539e95da0fd4abc8d3889dcefe8f1))
* domain structure finished ([b65c364](https://github.com/giovihey/UFG/commit/b65c364eb7df5308204df508789f964fa5322f31))
* finish game engine and game loop interaction ([3b06ff7](https://github.com/giovihey/UFG/commit/3b06ff7ce473f6578d6c8d0362a54d445c71bc4f))
* fuck you detekt ([9bc109b](https://github.com/giovihey/UFG/commit/9bc109bb63aae089f4122459383e870e8988dfb5))
* keyboard inputs transfered using compose UI library ([2df7146](https://github.com/giovihey/UFG/commit/2df7146886dd439174bfdc72ddaf25ac54211ea7))
* link the ui rendering (now the initial structure is finished) ([d0e79f4](https://github.com/giovihey/UFG/commit/d0e79f4024332e294873b0317d710593f7870267))
* update physicsSystem with new physics state ([cbb3950](https://github.com/giovihey/UFG/commit/cbb39500f3df4adf095d36fbfdfefcebc4478868))
* we have a UI ([81929a1](https://github.com/giovihey/UFG/commit/81929a1fde0a16de5f05b0f493fe98f7238eb51c))

### Bug Fixes

* fix TimeManager with accumulator for lag ([9187bc3](https://github.com/giovihey/UFG/commit/9187bc3ebc17e6b99697dabd743d74c5be45fbb7))
* remove dependency from domain and get better standard names ([9ccd4cf](https://github.com/giovihey/UFG/commit/9ccd4cfa62ea8aeae393b600920e35b563ffa1d2))

## [3.0.0](https://github.com/giovihey/UFG/compare/v2.3.1...v3.0.0) (2025-11-30)

### ⚠ BREAKING CHANGES

* define the architecture

### Features

* add Input classes ([7ca3570](https://github.com/giovihey/UFG/commit/7ca3570e0c88e10c34b005a40ff34bb188c9c32b))
* add keyboard adapter ([df8b7fd](https://github.com/giovihey/UFG/commit/df8b7fde1ebdbf6401197e36b0566884edb2ee94))
* add MoveInput and default Character class ([5750bfc](https://github.com/giovihey/UFG/commit/5750bfcae4cef0a438270f0083593f1381409739))
* add NetworkInput dto ([6137e6f](https://github.com/giovihey/UFG/commit/6137e6fffd7305ff0eb5a4d6cf16cacbf90fcd68))
* add some domain classes ([9572eb9](https://github.com/giovihey/UFG/commit/9572eb9d9a9ef4e487212580e1282b564d55e0f3))
* define the architecture ([6d21d44](https://github.com/giovihey/UFG/commit/6d21d44bf9de2e0329407be7fa46878cc44d84e5))
* start working on gameloop ([e03168a](https://github.com/giovihey/UFG/commit/e03168a19ee77c61d828d7ca8b865bc34a54c166))

## [2.3.1](https://github.com/giovihey/UFG/compare/v2.3.0...v2.3.1) (2025-11-15)

### Bug Fixes

* run only on Linux ([f6ee680](https://github.com/giovihey/UFG/commit/f6ee68056794cb0de7b3000f10b9106d899b3c14))

## [2.3.0](https://github.com/giovihey/UFG/compare/v2.2.0...v2.3.0) (2025-11-15)

### Features

* add a ci for a project ([1789a67](https://github.com/giovihey/UFG/commit/1789a677d2ac57cc4028a9223437af4be706dcfb))

## [2.2.0](https://github.com/giovihey/UFG/compare/v2.1.0...v2.2.0) (2025-11-15)

### Features

* add Hello World to the game ([026cbea](https://github.com/giovihey/UFG/commit/026cbea6b1c89cf9ab3a12094a76c6696cc2f4d1))

### Bug Fixes

* catcha approved ([daafaa0](https://github.com/giovihey/UFG/commit/daafaa0c304d09feb6d9c551a395a10b6dd8d969))
* the Hello world was unnecessary ([062e349](https://github.com/giovihey/UFG/commit/062e349888d63efe2fb5cf0fbb8a72d59cd54db6))

## [2.1.0](https://github.com/giovihey/UFG/compare/v2.0.0...v2.1.0) (2025-11-15)

### Features

* change game ([b1b9614](https://github.com/giovihey/UFG/commit/b1b9614336006d86d019ba81c3784b4cdc86dff8))

### Bug Fixes

* remove junk from readme ([bfd8457](https://github.com/giovihey/UFG/commit/bfd8457ed57d1c8fa776e604a8df39fa92676b43))

## [2.0.0](https://github.com/giovihey/UFG/compare/v1.2.3...v2.0.0) (2025-11-15)

### ⚠ BREAKING CHANGES

* pls change the release
* we love versioning

### Features

* pls change the release ([983f910](https://github.com/giovihey/UFG/commit/983f9104dcb6789ca3951b8cba28f6ea5872520a))
* we love versioning ([c46146c](https://github.com/giovihey/UFG/commit/c46146c614c4c4bc386747b4f2b495dfb8cf1d75))

## [1.2.3](https://github.com/giovihey/UFG/compare/v1.2.2...v1.2.3) (2025-11-15)


### Bug Fixes

* merge workflow for release and jarBuild ([1f685dc](https://github.com/giovihey/UFG/commit/1f685dc49296c4807825a72de2aaf64ed82c137b))

## [1.2.2](https://github.com/giovihey/UFG/compare/v1.2.1...v1.2.2) (2025-11-15)


### Bug Fixes

* remove maven, we don't have maven ([edd433e](https://github.com/giovihey/UFG/commit/edd433e33f4b0cac091621c80b44a8fa7a40a821))

## [1.2.1](https://github.com/giovihey/UFG/compare/v1.2.0...v1.2.1) (2025-11-15)


### Bug Fixes

* convert ci build from ubuntu specific version to latest ([895ca45](https://github.com/giovihey/UFG/commit/895ca450e58238015a94b5a5f12c03c6ea222a4a))

# [1.2.0](https://github.com/giovihey/UFG/compare/v1.1.0...v1.2.0) (2025-11-15)


### Bug Fixes

* configuration refactoring and version change ([7a9f9d3](https://github.com/giovihey/UFG/commit/7a9f9d34805478e0e7c2df4d1f24ac73fc1851cb))


### Features

* add first gradle config ([285c23d](https://github.com/giovihey/UFG/commit/285c23dff46939eb5aa99135ef91cfca51a2cf18))
* gradle project setup finished ([b9b2b9c](https://github.com/giovihey/UFG/commit/b9b2b9cffd0983403695674aa48cbd535ca12fbf))

# [1.1.0](https://github.com/giovihey/UFG/compare/v1.0.0...v1.1.0) (2025-11-15)


### Features

* ciao, new release ([b352649](https://github.com/giovihey/UFG/commit/b3526490abbde8af44e68662dda1e2e11b38a987))

# 1.0.0 (2025-11-15)


### Bug Fixes

* add ci enhancement ([931b6f5](https://github.com/giovihey/UFG/commit/931b6f576c4d3d91da221fe83079e82805c30efb))


### Features

* add first gradle config ([4db1fad](https://github.com/giovihey/UFG/commit/4db1fade575d8e13bdefac2360612a1065c70808))

# 1.0.0 (2025-11-15)


### Bug Fixes

* add ci enhancement ([931b6f5](https://github.com/giovihey/UFG/commit/931b6f576c4d3d91da221fe83079e82805c30efb))


### Features

* add first gradle config ([4db1fad](https://github.com/giovihey/UFG/commit/4db1fade575d8e13bdefac2360612a1065c70808))
