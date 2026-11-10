# bdFun

<p align="center">
  <img src="docs/icon.png" width="260" alt="bdFun">
</p>

<p align="center">
  <a href="https://github.com/QxTeak/bdFun/releases"><img src="https://img.shields.io/github/v/release/QxTeak/bdFun?include_prereleases&label=release&color=FF6699&style=flat-square" alt="release"></a>
  <a href="https://github.com/QxTeak/bdFun/releases"><img src="https://img.shields.io/github/downloads/QxTeak/bdFun/total?label=downloads&color=4CAF50&style=flat-square" alt="downloads"></a>
  <img src="https://img.shields.io/github/license/QxTeak/bdFun?label=license&color=FFA500&style=flat-square" alt="license">
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white&style=flat-square" alt="platform">
  <img src="https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white&style=flat-square" alt="kotlin">
  <img src="https://img.shields.io/badge/Compose-Jetpack-4285F4?logo=jetpackcompose&logoColor=white&style=flat-square" alt="compose">
  <img src="https://img.shields.io/badge/build-Gradle-02303A?logo=gradle&logoColor=white&style=flat-square" alt="build">
</p>

bdFun 鏄竴涓涓夋柟鍝斿摡鍝斿摡 Android 瀹㈡埛绔紝浣跨敤 Kotlin + Jetpack Compose 缂栧啓锛岃嚧鍔涗簬杞诲揩銆佹祦鐣呫€佺畝娲併€佺編瑙傜殑浣跨敤浣撻獙銆?
> 鏈」鐩负绗笁鏂归潪瀹樻柟瀹㈡埛绔紝涓庡摂鍝╁摂鍝╋紙涓婃捣瀹藉ū鏁扮爜绉戞妧鏈夐檺鍏徃锛夊強鍏跺叧鑱旀柟鏃犱换浣曢毝灞炪€佹巿鏉冩垨鍚堜綔鍏崇郴銆傞」鐩粎鐢ㄤ簬瀛︿範銆佺爺绌朵笌鎶€鏈氦娴侊紝涓嶆彁渚涖€佷笉鍖呭惈銆佷笉浼犳挱浠讳綍鐮磋В鍐呭銆佷粯璐瑰唴瀹圭粫杩囨満鍒舵垨浼氬憳鏉冪泭鐮磋В鍔熻兘銆?
## 鐣岄潰棰勮

<p align="center">
  <img src="docs/preview.jpg" width="100%" alt="bdFun 鐣岄潰棰勮">
</p>

閲囩敤 HyperOS 4 鐨勮璁￠鏍笺€?
## 瀹夎

绯荤粺瑕佹眰锛欰ndroid 13+

## 鍙嶉

娆㈣繋閫氳繃 Issue 鍙嶉闂銆佹彁鍑哄缓璁紝涔熷彲浠ュ姞鍏ユ垜浠殑 [Telegram 缇ょ粍](https://t.me/+tQrDYf-hb702ZjM1)銆?
## 鏋勫缓

鐜锛?
- JDK 21
- Android SDK Platform 37 / Build Tools 37.0.0
- Gradle Wrapper锛堜粨搴撹嚜甯︼級

```bash
# Linux / macOS
./gradlew :app:assembleDebug

# Windows
gradlew.bat :app:assembleDebug
```

> Note: this project builds against a local Miuix source snapshot.
> Place it at `../miuix-src/miuix-45645854cd46c32275c5666e3aafa9980d8f104e`
> or set the `MIUIX_SOURCE` environment variable to the Miuix repo path.

Release锛?
```bash
./gradlew :app:assembleRelease
```

濡傛灉鏈満娌℃湁閰嶇疆 Android SDK锛岃鍦ㄩ」鐩牴鐩綍鍒涘缓 `local.properties`锛?
```properties
sdk.dir=/path/to/Android/Sdk
```

## 椤圭洰缁撴瀯

```text
app/              涓诲簲鐢紙鍖呭悕 com.aritxonly.bdfun锛?TeakUI/           Compose 椤甸潰鏁堟灉缁勪欢搴?danmaku-engine/   鍩轰簬 BiliPai / bytedance DanmakuRenderEngine 鐨勫脊骞曟覆鏌撴ā鍧?```

TeakUI 鏄负鏂逛究寮€鍙戣€屾娊鍑虹殑 Compose UI 缁勪欢搴擄細鎶婇〉闈㈤鏋躲€佺幓鐠冩潗璐ㄣ€侀《閮ㄧ粍浠跺拰鍔ㄦ晥缁熶竴鏀舵暃鎴愪竴濂楃粍浠讹紝寮€鍙戞椂鐩存帴璋冪敤锛岄伩鍏嶆瘡涓〉闈㈤噸澶嶅疄鐜般€傚畠鍖呭惈鍩轰簬 Hyper-PiliPlus 鐨?HyperOS 4 椋庢牸 Tab 鏍忋€侀《閮ㄧ幓鐠冪粍浠跺姩鐢伙紝浠ュ強鍩轰簬 Miuix 鐨勭粺涓€椤甸潰鑳藉姏锛屽寘鎷笎杩涘紡妯＄硦銆佸ぇ/灏忔爣棰樻姌鍙犲姩鐢荤瓑銆?
`danmaku-engine` 鏄?4.0 寮曞叆鐨勭嫭绔嬪脊骞曟覆鏌撴ā鍧楋紝璐熻矗寮瑰箷瑙ｆ瀽銆佽建閬撹绠楀拰缁樺埗锛屼繚鐣欎笂娓?Apache-2.0 鐗堟潈涓庤鍙鏄庛€?
鏇村椤甸潰瑙勮寖瑙?`TeakUI/docs/PAGE_CHROME_SPEC.md`锛岀涓夋柟鏉ユ簮娓呭崟瑙?`TeakUI/docs/PROVENANCE.md`銆?
## 鑷磋阿

bdFun 浣跨敤浜?/ 鍙傝€冧簡浠ヤ笅椤圭洰锛屾劅璋綔鑰呭拰绀惧尯鐨勫伐浣滐細

- [Hyper-PiliPlus](https://github.com/AritxOnly/Hyper-PiliPlus)锛圼@AritxOnly](https://github.com/AritxOnly)锛宖ork [@Mutx163](https://github.com/Mutx163)锛夛細鐜荤拑鏉愯川銆佹姌灏勩€侀槾褰便€佸簳鏍忓脊绨с€丳rogressiveTopBar 绛?- [HyperIsland](https://github.com/1812z/HyperIsland)锛圼@1812z](https://github.com/1812z)锛夛細娓愯繘妯＄硦鍙傛暟涓庤鎰熷弬鑰?- [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus)锛圼@bggRGjQaUbCoE](https://github.com/bggRGjQaUbCoE)锛夛細鍔熻兘涓庝氦浜掑弬鑰?- [Miuix](https://github.com/compose-miuix-ui/miuix)锛圼@compose-miuix-ui](https://github.com/compose-miuix-ui)锛夛細Compose UI / 鐜荤拑 blur 渚濊禆
- [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)锛圼@SocialSisterYi](https://github.com/SocialSisterYi)锛夛細鍝斿摡鍝斿摡鎺ュ彛璧勬枡
- [BiliPai](https://github.com/jay3-yy/BiliPai) / [bytedance DanmakuRenderEngine](https://github.com/bytedance/DanmakuRenderEngine)锛氬脊骞曟覆鏌撴ā鍧椼€佸脊骞曠┖闄嶅姪鎵嬨€佸脊骞曡鏁板疄鐜扮Щ妞嶏紝姘存淮鍔ㄧ敾
- [FodFluidRipple](https://github.com/Leaf-lsgtky)锛圼@Leaf-lsgtky](https://github.com/Leaf-lsgtky)锛夛細姘存尝绾瑰姩鐢?- [Guiyuan](https://github.com/CHS-Haple/Guiyuan)锛圼@CHS-Haple](https://github.com/CHS-Haple)锛夛細寮€濮嬮〉绉绘
- HyperOS 4锛氫竴闀滃埌搴曞姩鏁?- AndroidX / Jetpack Compose銆丮edia3 / ExoPlayer銆丱kHttp銆丆oil銆並otlin / kotlinx.coroutines銆乑Xing

鎰熻阿鎵€鏈変笂娓搁」鐩綔鑰呬笌璐＄尞鑰呫€?
## 璁稿彲璇?
鏈」鐩互 **GNU General Public License v3.0** 鍙戝竷锛屽畬鏁存枃鏈 [LICENSE](LICENSE)銆?
Copyright (C) 2026 bdFun contributors

浣犲彲浠ヨ嚜鐢变娇鐢ㄣ€佷慨鏀广€佸啀鍒嗗彂锛屼絾闇€瑕侊細

1. 淇濈暀 `LICENSE`銆乣THIRD_PARTY_NOTICES.md`銆乣TeakUI/docs/PROVENANCE.md`锛?2. 瀵瑰鍒嗗彂淇敼鐗堟椂锛屾寜 GPL-3.0 鎻愪緵瀵瑰簲瀹屾暣婧愮爜锛?3. 涓嶉澶栨坊鍔犱笌 GPL-3.0 鍐茬獊鐨勯檺鍒躲€?
绗笁鏂逛緷璧栦笌璁稿彲璇存槑瑙?[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)銆?
## 鍏嶈矗澹版槑

- 鏈」鐩寜鈥滅幇鐘垛€濇彁渚涳紝涓嶅绋冲畾鎬с€佸彲鐢ㄦ€с€佸畨鍏ㄦ€т綔浠讳綍淇濊瘉銆?- 鎵€鏈夎棰戙€侀煶棰戙€佸浘鐗囥€佸脊骞曠瓑鍐呭鐗堟潈褰掑摂鍝╁摂鍝╁強鐩稿簲鏉冨埄浜烘墍鏈夛紝鏈」鐩笉涓诲紶浠讳綍鍐呭鏉冨埄銆?- 鍥犲畼鏂规帴鍙ｅ彉鏇淬€佺瓥鐣ヨ皟鏁存垨涓嶅彲鎶楀姏瀵艰嚧鐨勫姛鑳藉け鏁堟垨鎹熷け锛屽紑鍙戣€呬笉鎵挎媴璐ｄ换銆?- 浣跨敤鏈」鐩嵆琛ㄧず浣犲凡闃呰骞跺悓鎰忓畬鏁寸殑浣跨敤椤荤煡锛涘涓嶅悓鎰忥紝璇峰仠姝娇鐢ㄥ苟鍒犻櫎鏈」鐩€?