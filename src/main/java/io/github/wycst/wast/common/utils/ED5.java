package io.github.wycst.wast.common.utils;

/**
 * @Date 2024/5/24 18:22
 * @Created by wangyc
 */
class ED5 {

    final static ED5[] ED5_A;

    static {
        ED5_A = new ED5[343];
        long[][] ed5datas = {{1,0,0,0x8000000000000000L,31,0},{5,0,0,0x6666666666666666L,33,1717986918},{25,0,0,0x51eb851eb851eb85L,35,515396075},{125,0,0,0x4189374bc6a7ef9dL,37,-1305670058},{625,0,0,0x68db8bac710cb295L,40,-371085175},{0xc35L,0,0,0x53e2d6238da3c211L,42,-2014855058},{0x3d09L,0,0,0x431bde82d7b634daL,44,-752890588},{0x1312dL,0,0,0x6b5fca6af2bd215eL,47,513361979},{0x5f5e1L,0,0,0x55e63b88c230e77eL,49,2128676501},{0x1dcd65L,0,0,0x44b82fa09b5a52cbL,51,-1733032636},{0x9502f9L,0,0,0x6df37f675ef6eadfL,54,1522115079},{0x2e90eddL,0,0,0x57f5ff85e592557fL,56,2076685522},{0xe8d4a51L,0,0,0x465e6604b7a84465L,58,-56638501},{0x48c27395L,0,0,0x709709a125da0709L,61,-1808608519},{0x16bcc41e9L,0,0,0x5a126e1a84ae6c07L,63,-1446886816},{0x71afd498dL,0,0,0x480ebe7b9d58566cL,65,-2016502912},{0x2386f26fc1L,0,0,0x734aca5f6226f0adL,68,-1508417740},{0xb1a2bc2ec5L,0,0,0x5c3bd5191b525a24L,70,-2065727652},{0x3782dace9d9L,0,0,0x49c97747490eae83L,72,-1652582121},{0x1158e460913dL,0,0,0x760f253edb4ab0d2L,75,-1785137935},{0x56bc75e2d631L,0,0,0x5e72843249088d75L,77,1148870030},{0x1b1ae4d6e2ef5L,0,0,0x4b8ed0283a6d3df7L,79,1778089483},{0x878678326eac9L,0,0,0x78e480405d7b9658L,82,-1450024123},{0x2a5a058fc295edL,0,0,0x60b6cd004ac94513L,84,-1160019298},{0xd3c21bcecceda1L,0,0,0x4d5f0a66a23a9da9L,86,1648964939},{0x422ca8b0a00a425L,0,0,0x7bcb43d769f762a8L,89,-1656623394},{0x14adf4b7320334b9L,0,0,0x63090312bb2c4eedL,91,1251681663},{0x6765c793fa10079dL,0,0,0x4f3a68dbc8f03f24L,93,1001345330},{0x409f9cbc7c4a04c2L,0x20000000L,3,0x7ec3daf941806506L,96,-974827849},{0x50c783eb9b5c85f2L,-1476395008,5,0x65697bfa9acd1d9fL,98,79131180},{0x64f964e68233a76fL,0x52000000L,7,0x51212ffbaf0a7e18L,100,-795688516},{0x7e37be2022c0914bL,0x26800000L,9,0x40e7599625a1fe7aL,102,1081436106},{0x4ee2d6d415b85aceL,-133169152,12,0x67d88f56a29cca5dL,105,871304310},{0x629b8c891b267182L,-1240203264,14,0x5313a5dee87d6eb0L,107,-161950011},{0x7b426fab61f00de3L,0x63990000L,16,0x42761e4bed31255aL,109,1588426909},{0x4d0985cb1d3608aeL,0x1e3fa000L,19,0x6a5696dfe1e83bc3L,112,-894490781},{0x604be73de4838ad9L,-1513125888,21,0x5512124cb4b9c969L,114,1861387752},{0x785ee10d5da46d90L,0xf436a00L,23,0x440e750a2a2e3abaL,116,-1087870176},{0x4b3b4ca85a86c47aL,0x98a2240L,26,0x6ce3ee76a9e3912aL,119,-881598822},{0x5e0a1fd271287598L,-1947424048,28,0x571cbec554b60dbbL,121,-705279057},{0x758ca7c70d7292feL,-1360538236,30,0x45b0989ddd5e7163L,123,294770213},{0x4977e8dc68679bdfL,0x2d50e572L,33,0x6f80f42fc8971bd1L,126,-1246354577},{0x5bd5e313828182d6L,-123396401,35,0x5933f68ca078e30eL,128,720903257},{0x72cb5bd86321e38cL,-1227987326,37,0x475cc53d4d2d8271L,130,-1141264313},{0x47bf19673df52e37L,-230621167,40,0x722e086215159d82L,133,-967029442},{0x59aedfc10d7279c5L,-288276458,42,0x5b5806b4ddaae468L,135,-1632617013},{0x701a97b150cf1837L,0x6a85901bL,44,0x49133890b1558386L,137,-447100151},{0x46109eced2816f22L,-1567393263,47,0x74eb8db44eef38d7L,140,-715360241},{0x5794c6828721caebL,0x4b385895L,49,0x5d893e29d8bf60acL,142,-1431281652},{0x6d79f82328ea3da6L,0x1e066ebbL,51,0x4ad431bb13cc4d56L,144,-286031863},{0x446c3b15f9926687L,-758905548,54,0x77b9e92b52e07bbeL,147,1260335938},{0x558749db77f70029L,-948631934,56,0x5fc7edbc424d2fcbL,149,1867262210},{0x6ae91c5255f4c034L,0x39524822L,58,0x4c9ff163683dbfd5L,151,-224177151},{0x42d1b1b375b8f820L,-1546425067,61,0x7a998238a6c932efL,154,-358683441},{0x53861e2053273628L,-859289509,63,0x6214682d523a8f26L,156,1431040166},{0x6867a5a867f103b2L,0xfffa5a71L,65,0x4e76b9bddb620c1eL,158,-1432148245},{0x4140c78940f6a24fL,-537102201,68,0x7d8ac2c95f034697L,161,2003530104},{0x5190f96b91344ae3L,-671377751,70,0x646f023ab2690545L,163,-115162835},{0x65f537c675815d9cL,-839222189,72,0x5058ce955b87376bL,165,766863191},{0x7f7285b812e1b504L,0x1791b68L,74,0x40470baaaf9f5f88L,167,-245502907},{0x4fa793930bcd1122L,-2132037343,77,0x66d812aab29898dbL,170,466188809},{0x63917877cec0556bL,0x21269d69L,79,0x524675555bad4715L,172,-1345035871},{0x7c75d695c2706ac5L,-378518333,81,0x41d1f7777c8a9f44L,174,-1935022156},{0x4dc9a61d998642bbL,-1310315782,84,0x694ff258c7443207L,177,1198931846},{0x613c0fa4ffe7d36aL,-1637894728,86,0x543ff513d29cf4d2L,179,-1617834901},{0x798b138e3fe1c845L,0x45f7a327L,88,0x43665da9754a5d75L,181,1282712457},{0x4bf6ec38e7ed1d2bL,0x4bbac5f8L,91,0x6bd6fc425543c8bbL,184,-1383633906},{0x5ef4a74721e86476L,0x1ea97776L,93,0x5645969b77696d62L,186,-247913665},{0x76b1d118ea627d93L,-1504455340,95,0x4504787c5f878ab5L,188,-1916317851},{0x4a2f22af927d8e7cL,0x47f46554L,98,0x6e6d8d93cc0c1122L,191,2087852194},{0x5cbaeb5b771cf21bL,0x59f17ea9L,100,0x5857a4763cd6741bL,193,-1765692082},{0x73e9a63254e42ea2L,0x306dde54L,102,0x46ac8391ca4529afL,195,-1412553665},{0x487207df750e9d25L,0x5e44aaf4L,105,0x711405b6106ea919L,198,316894513},{0x5a8e89d75252446eL,-1244277327,107,0x5a766af80d255414L,200,253515611},{0x71322c4d26e6d58aL,0x634b4b1eL,109,0x485ebbf9a41ddcdcL,202,-656180971},{0x46bf5bb038504576L,0x7e0f0ef2L,112,0x73cac65c39c96161L,205,1527090825},{0x586f329c466456d4L,0x1d92d2afL,114,0x5ca23849c7d44de7L,207,2080666119},{0x6e8aff4357fd6c89L,0x24f7875bL,116,0x4a1b603b06437185L,209,-53454023},{0x4516df8a16fe63d5L,-1222986599,119,0x76923391a39f1c09L,212,-1803513356},{0x565c976c9cbdfccbL,0x24e161bfL,121,0x5edb5c7482e5b007L,214,-1442810685},{0x6bf3bd47c3ed7bfdL,-300303825,123,0x4be2b05d35848cd2L,216,-295255089},{0x4378564cda746d7eL,-1261431715,126,0x796ab3c855a0e151L,219,2104572236},{0x54566be0111188deL,0x66b47f77L,128,0x6122296d114d810dL,221,-34329130},{0x696c06d81555eb15L,-91938862,130,0x4db4edf0daa4673eL,223,1690523615},{0x41e384470d55b2edL,-1131203613,133,0x7c54afe7c43a3ecaL,226,986850865},{0x525c6558d0ab1fa9L,0x2bb800dcL,135,0x6376f31fd02e98a1L,228,-928506226},{0x66f37eaf04d5e793L,0x76a60113L,137,0x4f925c1973587a1bL,230,116188478},{0x40582f2d6305b0bcL,0x2a27c0acL,140,0x7f50935bebc0c35eL,233,1903888484},{0x506e3af8bbc71cebL,0x34b1b0d7L,142,0x65da0f7cbc9a35e5L,235,664117328},{0x6489c9b6eab8e426L,0x1de1d0cL,144,0x517b3f96fd482b1dL,237,-1186693056},{0x7dac3c24a5671d2fL,-2108316592,146,0x412f66126439bc17L,239,-949354445},{0x4e8ba596e760723dL,-1317697870,149,0x684bd683d38f9359L,242,1058013266},{0x622e8efca1388ecdL,0x1dd2e85eL,151,0x536fdecfdc72dc47L,244,1705404072},{0x7aba32bbc986b280L,0x6547a276L,153,0x42bfe57316c249d2L,246,-1212657120},{0x4cb45fb55df42f90L,0x3f4cc589L,156,0x6acca251be03a951L,249,636728985},{0x5fe177a2b5713b74L,0x4f1ff6ecL,158,0x557081dafe695440L,251,-349610271},{0x77d9d58b62cd8a51L,0x7bf7fcafL,160,0x445a017bfebaa9cdL,253,-1997675135},{0x4ae825771dc07672L,-573507352,163,0x6d5ccf2ccac442e2L,256,1957680539},{0x5da22ed4e530940fL,-1790626014,165,0x577d728a3bd03581L,258,-151842487},{0x750aba8a1e7cb913L,0x7a9684ebL,167,0x45fdf53b630cf79bL,260,737519469},{0x4926b496530df3acL,0x2c9e1313L,170,0x6ffcbb923814bf5eL,263,-1396949227},{0x5b7061bbe7d17097L,0x37c597d8L,172,0x5996fc74f9aa32b2L,265,600427537},{0x724c7a2ae1c5ccbdL,0x5b6fdceL,174,0x47abfd2a6154f55bL,267,1339335489},{0x476fcc5acd1b9ff6L,0x23925ea0L,177,0x72acc843ceee555eL,270,-434043595},{0x594bbf71806287f3L,-1401489848,179,0x5bbd6d030bf1dde5L,272,-2065221795},{0x6f9eaf4de07b29f0L,-1751862309,181,0x49645735a327e4b7L,274,-1652177436},{0x45c32d90ac4cfa36L,0x5ebcf069L,184,0x756d5855d1d96df2L,277,-1784490438},{0x5733f8f4d76038c3L,-160682877,186,0x5df11377db1457f5L,279,1149388027},{0x6d00f7320d3846f4L,-200853596,188,0x4b2742c648dd132aL,281,-1657469956},{0x44209a7f48432c59L,0x188482c6L,191,0x783ed13d4161b844L,284,784021907},{0x5528c11f1a53f76fL,0x5ea5a378L,193,0x603240fdcde7c69cL,286,-231775933},{0x6a72f166e0e8f54bL,0x364f0c56L,195,0x4cf500cb0b1fd217L,288,673572712},{0x4287d6e04c91994fL,0x1f167b5L,198,0x7b219ade7832e9beL,291,-1499264038},{0x5329cc985fb5ffa2L,-1032994397,200,0x628148b1f9c25498L,293,-2058404689},{0x67f43fbe77a37f8bL,0x7309320cL,202,0x4ecdd3c1949b76e0L,295,1789250085},{0x40f8a7d70ac62fb7L,0x27e5bf47L,205,0x7e161f9c20f8be33L,298,-573173700},{0x5136d1cccd77bba4L,-237031655,207,0x64de7fb01a609829L,300,2118441417},{0x6584864000d5aa8eL,0x2e56fadfL,209,0x50b1ffc0151a1354L,302,1694753134},{0x7ee5a7d0010b1531L,-1175668329,211,0x408e66334414dc43L,304,-2080171330},{0x4f4f88e200a6ed3fL,0x1433f3feL,214,0x674a3d1ed354939fL,307,966693169},{0x63236b1a80d0a88eL,-650055426,216,0x52a1ca7f0f76dc7fL,309,1632347994},{0x7bec45e12104d2b2L,-1886311106,218,0x421b0865a5f8b065L,311,-412108523},{0x4d73abacb4a303afL,-1715815354,221,0x69c4da3c3cc11a3cL,314,-1518367096},{0x60d09697e1cbc49bL,-2144769192,223,0x549d7b6363cdae96L,316,-355700218},{0x7904bc3dda3eb5c2L,0x6033c62eL,225,0x43b12f82b63e2545L,318,-2002547093},{0x4ba2f5a6a8673199L,0x7c205bddL,228,0x6c4eb26abd303ba2L,321,1949885407},{0x5e8bb3105280fdffL,-618106156,230,0x56a55b889759c94eL,323,-1017072052},{0x762e9fd467213d7fL,-772632695,232,0x45511606df7b0772L,325,904329277},{0x49dd23e4c074c66fL,-482895434,235,0x6ee8233e325e7250L,328,1446926843},{0x5c546cddf091f80bL,-603619293,237,0x58b9b5cb5b7ec1d9L,330,-560445444},{0x736988156cb6760eL,-754524116,239,0x46faf7d5e2cbce47L,332,-448356355},{0x4821f50d63f209c9L,0x43e44c1bL,242,0x71918c896adfb073L,335,141623291},{0x5a2a7250bcee8c3bL,-1797431518,244,0x5adad6d4557fc05cL,337,113298633},{0x70b50ee4ec2a2f4aL,0x7a14b6ebL,246,0x48af1243779966b0L,339,90638906},{0x4671294f139a5d8eL,-1941114285,249,0x744b506bf28f0ab3L,342,1004015709},{0x580d73a2d880f4f2L,0x2f602ee7L,251,0x5d090d2328726ef5L,344,-914774351},{0x6e10d08b8ea1322eL,-1153942879,253,0x4a6da41c205b8bf7L,346,-731819481},{0x44ca82573924bf5dL,0x3d8f2df5L,256,0x7715d36033c5acbfL,349,-1170911170},{0x55fd22ed076def34L,-2109477426,258,0x5f44a919c3048a32L,351,-77735477},{0x6b7c6ba849496b01L,-1563104958,260,0x4c36edae359d3b5bL,353,-62188381},{0x432dc3492dcde2e1L,0x5c511c9L,263,0x79f17c49ef61f893L,356,759492049},{0x53f9341b79415b99L,0x4736563bL,265,0x618dfd07f2b4c6dcL,358,607593639},{0x68f781225791b27fL,-1727796278,267,0x4e0b30d328909f16L,360,-2090905466},{0x419ab0b576bb0f8fL,-1079872674,270,0x7cdeb4850db431bdL,363,-1627461827},{0x52015ce2d469d373L,-1349840842,272,0x63e55d373e29c164L,365,2134004375},{0x6681b41b89844850L,-1687301053,274,0x4feab0f8fe87cde9L,367,-10783419},{0x4011109135f2ad32L,0x6124a4aaL,277,0x7fdde7f4ca72e30fL,370,-17253469},{0x501554b5836f587eL,-110244395,279,0x664b1ff7085be8d9L,372,-1731789694},{0x641aa9e2e44b2e9eL,-1211547318,281,0x51d5b32c06afed7aL,374,-526438296},{0x7d21545b9d5dfa46L,0x65bb919cL,283,0x4177c2899ef32462L,376,1296836281},{0x4e34d4b9425abc6bL,-6997246,286,0x68bf9da8fe51d3d0L,379,2074938051},{0x61c209e792f16b86L,-86561,288,0x53cc7e20cb74a973L,381,-1776023396},{0x7a328c6177adc668L,-1084675021,290,0x4309fe80a2c3bac2L,383,-561825258},{0x4c5f97bceacc9c01L,0x7797bb9fL,293,0x6b4330cdd1392ad1L,386,1678059965},{0x5f777dac257fc301L,-713184633,295,0x55cf5a3e40fa88a7L,388,-2093525865},{0x77555d172edfb3c2L,0x4add1529L,297,0x44a5e1cb672ed3b9L,390,902159686},{0x4a955a2e7d4bd059L,0x6eca2d3aL,300,0x6dd636123eb152c1L,393,-274531421},{0x5d3ab0ba1c9ec46fL,-897795960,302,0x57de91a832277567L,395,-219625137},{0x74895ce8a3c6758bL,-1122244949,304,0x464ba7b9c1b92ab9L,397,-1893687028},{0x48d5da11665c0977L,0x5631702aL,307,0x70790c5c6928445cL,400,406074592},{0x5b0b5095bff30bd5L,0x2bbdcc35L,309,0x59fa7049edb9d049L,402,-1393127245},{0x71ce24bb2fefcecaL,0x76ad3f42L,311,0x47fb8d07f161736eL,404,603485122},{0x4720d6f4fdf5e13eL,-1976809591,314,0x732c14d98235857dL,407,106582737},{0x58e90cb23d73598eL,0x2cb7596cL,316,0x5c2343e134f79dfdL,409,-1632720729},{0x6f234fdeccd02ff1L,-1209716793,318,0x49b5cfe75d92e4caL,411,-447183124},{0x457611eb40021df7L,0x12ef3ddcL,321,0x75efb30bc8eb07abL,414,143500461},{0x56d396661002a574L,-676655789,323,0x5e595c096d88d2efL,416,973793828},{0x6c887bff94034ed2L,0xd95d0a8L,325,0x4b7ab0078ad3dbf2L,418,-1797945315},{0x43d54d7fbc821143L,0x487da269L,328,0x78c44cd8de1fc650L,421,-299732127},{0x54caa0dfaba29594L,0x1a9d0b03L,330,0x609d0a4718196b73L,423,-239785701},{0x69fd4917968b3af9L,0x21444dc4L,332,0x4d4a6e9f467abc5cL,425,-1050822020},{0x423e4daebe1704dbL,-1261784934,335,0x7baa4a9870c46094L,428,1754658604},{0x52cde11a6d9cc612L,-1577231167,337,0x62eea2138d69e6ddL,430,544733424},{0x678159610903f797L,0x4a7cb3f2L,339,0x4f254e760abb1f17L,432,1294780199},{0x40b0d7dca5a27abeL,-1903300489,342,0x7ea21723445e9825L,435,1212654859},{0x50dd0dd3cf0b196eL,0x32316c95L,344,0x654e78e9037ee01dL,437,-747863032},{0x65145148c2cddfc9L,-1094858822,346,0x510b93ed9c658017L,439,-598290425},{0x7e59659af38157bcL,0x2e6d39a9L,348,0x40d60ff149eaccdfL,441,-478632340},{0x4ef7df80d830d6d5L,-1660664823,351,0x67bce64edcaae166L,444,952175174},{0x62b5d7610e3d0c8bL,0x445550cL,353,0x52fd850be3bbe784L,446,-97253320},{0x7b634d3951cc4fadL,-984176049,355,0x42646a6fe9631f9dL,448,-1795789574},{0x4d1e1043d31fb1ccL,-1688851855,358,0x6a3a43e642383295L,451,-1155276400},{0x60659454c7e79e3fL,-1037322995,360,0x54fb698501c68edeL,453,793765798},{0x787ef969f9e185cfL,-1296653743,362,0x43fc546a67d20be4L,455,-223980821},{0x4b4f5be23c2cf3a1L,-810408590,365,0x6cc6ed770c83463bL,458,500624146},{0x5e2332dacb38308aL,0x439eaecfL,367,0x57058ac5a39c382fL,460,1259492776},{0x75abff917e063cacL,-729392509,369,0x459e089e1c7cf9bfL,462,1866587680},{0x498b7fbaeec3e5ecL,0x4d3f892L,372,0x6f6340fcfa618f98L,465,-1308427008},{0x5bee5fa9aa74df67L,0x608f6b6L,374,0x591c33fd951ad946L,467,-187748147},{0x72e9f79415121740L,-947178396,376,0x4749c33144157a9fL,469,708794941},{0x47d23abc8d2b4e88L,0x7cb700beL,379,0x720f9eb539bbf765L,472,275078447},{0x59c6c96bb076222aL,-1679507218,381,0x5b3fb22a94965f84L,474,220062758},{0x70387bc69c93aab5L,0x42ddf129L,383,0x48ffc1bbaa11e603L,476,1035043665},{0x46234d5c21dc4ab1L,0x49cab6baL,386,0x74cc692c434fd66bL,479,-1779903972},{0x57ac20b32a535d5dL,-1673698200,388,0x5d705423690cab89L,481,1153057200},{0x6d9728dff4e834b5L,0x34cbd82L,390,0x4ac0434f873d5607L,483,1781439219},{0x447e798bf91120f1L,0x220ff671L,393,0x779a054c0b955672L,486,1132315832},{0x559e17eef755692dL,0x6a93f40eL,395,0x5fae6aa33c77785bL,488,1764846125},{0x6b059deab52ac378L,-986124015,397,0x4c8b888296c5f9e2L,490,-1165103478},{0x42e382b2b13aba2bL,0x7b4396abL,400,0x7a78da6a8ad65c9dL,493,-146178646},{0x539c635f5d8968b6L,0x5a147c56L,402,0x61fa48553bdeb07eL,495,1601044002},{0x68837c3734ebc2e3L,-258368661,404,0x4e61d37763188d31L,497,-437151717},{0x41522da2811359ceL,0x76600123L,407,0x7d6952589e8daeb6L,500,1018544171},{0x51a6b90b21583042L,0x13f8016bL,409,0x645441e07ed7bef8L,502,814835337},{0x6610674de9ae3c52L,-1728708154,411,0x504367e6cbdfcbf9L,504,-1066118649},{0x7f9481216419cb67L,0x3f338238L,413,0x4035ecb8a3196ffbL,506,6098540},{0x4fbcd0b4de901f20L,-138249757,416,0x66bcadf43828b32bL,509,868751123},{0x63ac04e2163426e8L,-1453310532,418,0x52308b29c686f5bcL,511,695000898},{0x7c97061a9bc130a2L,-742896341,420,0x41c06f549ed25e30L,513,556000719},{0x4dde63d0a158be65L,-1001181125,423,0x6933e554315096b3L,516,1748594609},{0x6155fcc4c9aeedffL,0x3567fc49L,425,0x542984435aa6def5L,518,-319111231},{0x79ab7bf5fc1aa97fL,0x2c1fb5cL,427,0x435469cf7bb8b25eL,520,1462697934},{0x4c0b2d79bd90a9efL,0x61b93d19L,430,0x6bba42e592c11d63L,523,-1095657143},{0x5f0df8d82cf4d46bL,0x3a278c60L,432,0x562e9beadbcdb11cL,525,-1735519174},{0x76d1770e38320986L,0x8b16f78L,434,0x44f216557ca48db0L,527,2047558498},{0x4a42ea68e31f45f3L,-982587989,437,0x6e5023bbfaa0e2b3L,530,-159880240},{0x5cd3a5031be71770L,-1228234987,439,0x58401c96621a4ef6L,532,1590082726},{0x74088e43e2e0dd4cL,-461551909,441,0x4699b0784e7b725eL,534,-1304914197},{0x488558ea6dcc8a50L,0xece4c49L,444,0x70f5e726e3f8b6fdL,537,-369875796},{0x5aa6af25093face4L,0x1281df5bL,446,0x5a5e5285832d5f31L,539,-2013887555},{0x71505aee4b8f981dL,0x5727ff3eL,448,0x484b75379c244c27L,541,-1611110044},{0x46d238d4ef39bf12L,0x2e75767fL,451,0x73abeebf603a1372L,544,-1718782611},{0x5886c70a2b082ed6L,-1173171169,453,0x5c898bcc4cfb42c2L,546,342960829},{0x6ea878ccb5ca3a8cL,0x68978927L,455,0x4a07a309d72f689bL,548,1133362122},{0x45294b7ff19e6497L,-1050757704,458,0x76729e762518a75eL,551,-763600981},{0x56739e5fee05fdbdL,-1313447130,460,0x5ec2185e8413b918L,553,-1469874244},{0x6c1085f7e9877d2dL,0x1e23fbf0L,462,0x4bce79e536762dadL,555,1401080982},{0x438a53baf1f4ae3cL,0x32d67d76L,465,0x794a5ca1f0bd15e2L,558,523742653},{0x546ce8a9ae71d9cbL,0x3f8c1cd3L,467,0x61084a1b26fdab1bL,560,1277987582},{0x698822d41a0e503eL,0xf6f2408L,469,0x4da03b48ebfe227cL,562,1022390065},{0x41f515c49048f226L,-911903099,472,0x7c33920e46636a60L,565,1635824105},{0x52725b35b45b2eb0L,0x7c0ed426L,474,0x635c74d8384f884dL,567,449665824},{0x670ef2032171fa5cL,-1693284048,476,0x4f7d2a469372d370L,569,-499260800},{0x40695741f4e73c79L,-521431618,479,0x7f2eaa0a85848581L,572,1778163098},{0x5083ad1272210b98L,0x59267b2dL,481,0x65beee6ed136d134L,574,1422530478},{0x64a498570ea94e7eL,0x6f7019f9L,483,0x51658b8bda9240f6L,576,-1438955995},{0x7dcdbe6cd253a21eL,0xb4c2077L,485,0x411e093caedb672bL,578,-1151164796},{0x4ea0970403744552L,-955280310,488,0x68300ec77e2bd845L,581,-123876755},{0x6248bcc5045156a7L,0x78d3795dL,490,0x5359a56c64efe037L,583,-99101404},{0x7adaebf64565ac51L,0x570857b4L,492,0x42ae1df050bfe693L,585,779712336},{0x4cc8d379eb5f8bb2L,-698009904,495,0x6ab02fe6e79970ebL,588,2106533197},{0x5ffb085866376e9fL,-1946254203,497,0x5559bfebec7ac0bcL,590,1685226557},{0x77f9ca6e7fc54a47L,0x6efe25a6L,499,0x4447ccbcbd2f0096L,592,-1228799132},{0x4afc1e850fdb4e6cL,-1520511096,502,0x6d3fadfac84b3424L,595,1469895226},{0x5dbb262653d22207L,-826897046,504,0x576624c8a03c29b6L,597,-1401064197},{0x7529efafe8c6aa89L,-1033621308,506,0x45eb50a08030215eL,599,-261857898},{0x493a35cdf17c2a96L,0x197e9e7aL,509,0x6fdee76733803564L,602,-1277966096},{0x5b88c3416ddb353bL,-1612823015,511,0x597f1f85c2ccf783L,604,-1022372877},{0x726af411c952028aL,0x87d5d79fL,513,0x4798e6049bd72c69L,606,1759082076},{0x4782d88b1dd34196L,-1796888893,516,0x728e3cd42c8b7a42L,609,1096544403},{0x59638eade54811fcL,0x3a1f1074L,518,0x5ba4fd768a092e9bL,611,1736228982},{0x6fbc72595e9a167bL,0x48a6d492L,520,0x4950cac53b3a8bafL,613,-2046990652},{0x45d5c777db204e0dL,0xd6844dbL,523,0x754e113b91f745e5L,616,-1557198124},{0x574b3955d1e86190L,0x50c25612L,525,0x5dd80dc941929e51L,618,1331221878},{0x6d1e07ab466279f4L,0x64f2eb96L,527,0x4b133e3a9adbb1daL,620,-1512002875},{0x4432c4cb0bfd8c38L,-1088957634,530,0x781ec9f75e2c4fc4L,623,1016769237},{0x553f75fdcefcef46L,-287455219,532,0x6018a192b1bd0c9cL,625,-45578070},{0x6a8f537d42bc2b18L,-1433060847,534,0x4ce0814227ca707dL,627,-1754449374},{0x4299942e49b59aefL,0x6a9d444aL,537,0x7b00ced03faa4d95L,630,-1089132080},{0x533ff939dc2301abL,0x4544955dL,539,0x62670bd9cc883e11L,632,1705674714},{0x680ff788532bc216L,0x1695bab4L,541,0x4eb8d647d6d364daL,634,-1212440607},{0x4109fab533fb594dL,-820117831,544,0x7df48a0c8aebd491L,637,637075407},{0x514c796280fa2fa1L,0x41a4f9ddL,546,0x64c3a1a3a25643a7L,639,1368653785},{0x659f97bb2138bb89L,-1844561836,548,0x509c814fb511cfb9L,641,235929568},{0x7f077da9e986ea6bL,-158218647,550,0x407d343fc40e3fc7L,643,1047737114},{0x4f64ae8a31f45283L,0x7a1b1c02L,553,0x672eb9ffa016cc71L,646,-41607536},{0x633dda2cbe716724L,0x58a1e302L,555,0x528bc7ffb345705bL,648,825707430},{0x7c0d50b7ee0dc0edL,0x6eca5bc3L,557,0x42096ccc8f6ac048L,650,-198427515},{0x4d885272f4c89894L,0x653e795aL,560,0x69a8ae1418aacd41L,653,-2035470942},{0x60ea670fb1fabeb9L,0x7e8e17b0L,562,0x5486f1a9ad557101L,655,948603624},{0x792500d39e796e67L,-567173732,564,0x439f27baf1112734L,657,758882899},{0x4bb72084430be500L,-354483583,567,0x6c31d92b1b4ea520L,660,1214212639},{0x5ea4e8a553cede41L,0x2596c322L,569,0x568e4755af721db3L,662,1830363570},{0x764e22cea8c295d1L,0x6efc73ebL,571,0x453e9f77bf8e7e29L,664,605297397},{0x49f0d5c129799da2L,-446838670,574,0x6eca98bf98e3fd0eL,667,-1608504543},{0x5c6d0b3173d8050bL,-1610761297,576,0x58a213cc7a4ffda5L,669,1290176743},{0x73884dfdd0ce064eL,-2040362701,578,0x46e80fd6c83ffe1dL,671,-685845524},{0x483530bea280c3f1L,0x13fd95c0L,581,0x71734c8ad9fffcfcL,674,-1956346297},{0x5a427cee4b20f4edL,0x58fcfb30L,583,0x5ac2a3a247fffd96L,676,-706083578},{0x70d31c29dde93228L,-1355007492,585,0x489bb61b6ccccadfL,678,294126596},{0x4683f19a2ab1bf59L,0x6d85a43dL,588,0x742c569247ae1164L,681,-388390905},{0x5824ee00b55e2f2fL,-924381875,590,0x5cf04541d2f1a783L,683,-310712724},{0x6e2e2980e2b5bafbL,-1155477344,592,0x4a59d101758e1f9cL,685,-1107563638},{0x44dcd9f08db194ddL,0x54f48264L,595,0x76f61b3588e365c7L,688,-1772101821},{0x5614106cb11dfa14L,-1439587587,597,0x5f2b48f7a0b5eb06L,690,300305461},{0x6b991487dd657899L,-725742660,599,0x4c22a0c61a2b226bL,692,1099237828},{0x433facd4ea5f6b60L,0x24f6c755L,602,0x79d1013cf6ab6a45L,695,899787066},{0x540f980a24f74638L,0x2e34792bL,604,0x617400fd9222bb6aL,697,-1857150725},{0x69137e0cae3517c6L,0x39c19776L,606,0x4df6673141b562bbL,699,-1485720580},{0x41ac2ec7ece12edbL,0xe418fea9L,609,0x7cbd71e869223792L,702,-1518159468},{0x52173a79e8197a92L,-585154988,611,0x63cac186ba81c60eL,704,-355534116},{0x669d0918621fd937L,-1805185559,613,0x4fd5679efb9b04d8L,706,-1143420752},{0x402225af3d53e7c2L,-1128240975,616,0x7fbbd8fe5f5e6e27L,709,-1829473202},{0x502aaf1b0ca8e1b3L,0x6bf082deL,618,0x662fe0cb7f7ebe86L,711,254408356},{0x64355ae1cfd31a20L,0x46eca395L,620,0x51bfe70932cbcb9eL,713,1921513603},{0x7d42b19a43c7e0a8L,0x58a7cc7bL,622,0x4166526dc23ca2e5L,715,678217423},{0x4e49af006a5cec69L,0x3768dfcdL,625,0x68a3b716039437d5L,718,226154419},{0x61dc1ac084f42783L,-2059200576,627,0x53b62c119c769310L,720,-678069924},{0x7a532170a6313164L,0x6693ddb0L,629,0x42f8234149f875a7L,722,316537520},{0x4c73f4e667debedeL,-1071879538,632,0x6b269ecedcc0bc3eL,725,-2070520346},{0x5f90f22001d66e96L,0x70238531L,634,0x55b87f0be3cd6365L,727,920564101},{0x77752ea8024c0a3cL,0xc2c667eL,636,0x449398d64fd782b7L,729,1595444740},{0x4aa93d29016f8665L,-2019835889,639,0x6db8f48a1958d125L,732,1693718124},{0x5d538c7341cb67feL,0xe982b012L,641,0x57c72a0814470db7L,734,-2080999337},{0x74a86f90123e41feL,-1545380841,643,0x4638ee6cdd05a492L,736,-805806011},{0x48e945ba0b66e93fL,0x266e198eL,646,0x705b171494d5d41eL,739,428697301},{0x5b2397288e40a38eL,-267804686,648,0x59e278dd43de434bL,741,1201951300},{0x71ec7cf2b1d0cc72L,-1408497682,650,0x47e860b1031835d5L,743,-756425878},{0x4733ce17af227fc7L,-1417181963,653,0x730d67819e8d22efL,746,-1210281405},{0x5900c19d9aeb1fb9L,-1771477454,655,0x5c0ab9347ed74f26L,748,749761794},{0x6f40f20501a5e7a7L,-66863169,657,0x49a22dc398ac3f51L,750,-1118177483},{0x458897432107b0c8L,-41789481,660,0x75d04938f446cbb5L,753,-71097054},{0x56eabd13e9499cfbL,0x3ce2edcdL,662,0x5e403a93f69f095eL,755,1661109275},{0x6ca56c58e39c043aL,0xc1ba940L,664,0x4b6695432bb26de5L,757,469893961},{0x43e763b78e4182a4L,0x479149c8L,667,0x78a4220512b7163bL,760,1610823797},{0x54e13ca571d1e34dL,0x59759c3aL,669,0x60834e6a755f44fcL,762,1288659037},{0x6a198bcece465c20L,-1345125559,671,0x4d35d8552ab29d96L,764,-1546053148},{0x424ff76140ebf994L,0x6de3e20dL,674,0x7b895a21ddea95bdL,767,-755698118},{0x52e3f5399126f7f9L,-1990403439,676,0x62d4481b17eede31L,769,1972421883},{0x679cf287f570b5f7L,-340520651,678,0x4f1039af4658b1c1L,771,718944047},{0x40c21794f96671baL,-212825407,681,0x7e805c4ba3c11c68L,774,1150310476},{0x50f29d7a37c00e29L,-1339773583,683,0x65337d094fcdb053L,776,1779241840},{0x652f44d8c5b011b4L,0x1c2dd8ceL,685,0x50f5fda10ca48d0fL,778,-2012580365},{0x7e7b160ef71c1621L,0x23394f01L,687,0x40c4cae73d5070d9L,780,966916085},{0x4f0cedc95a718dd4L,-1241263775,690,0x67a144a52ee71af5L,783,688072278},{0x62d0293bb10df149L,-477837895,692,0x52e76a1dbf1f48c4L,785,550457822},{0x7b84338a9d516d9cL,0x5c65f727L,694,0x4252bb4aff4c3a36L,787,-2136614120},{0x4d32a036a252e481L,-1178617224,697,0x6a1df877fee05d24L,790,17391245},{0x607f48444ae79da2L,0x282fa917L,699,0x54e4c6c665804a83L,792,872906455},{0x789f1a555da1850aL,-1304718500,701,0x43ea389eb799d535L,794,-1019661754},{0x4b6370755a84f326L,-1083228934,704,0x6ca9f43125c2eebcL,797,1804515030},{0x5e3c4c92b1262ff0L,0x5b3e8b20L,706,0x56ee5cf41e358bc9L,799,-274374894},{0x75cb5fb75d6fbbecL,0x720e2de8L,708,0x458b7d90182ad63bL,801,639493544},{0x499f1bd29a65d573L,-951526223,711,0x6f4595b359de2391L,804,-694797248},{0x5c06e2c740ff4ad0L,-1189407779,713,0x590477c2ae4b4fa7L,806,-555837799},{0x73089b79113f1d84L,-413017899,715,0x4736c635583c3fb9L,808,2132310138},{0x47e5612baac77273L,0x109d2785L,718,0x71f13d2226c6cc5bL,811,-24277615},{0x59deb97695794f0fL,-725323418,720,0x5b27641b5238a37cL,813,-878415551},{0x705667d43ad7a2d3L,-906654272,722,0x48ec5015db6082caL,815,1015254477},{0x463600e4a4c6c5c4L,0x5e397898L,725,0x74ad4cefc56737a9L,818,-93579754},{0x57c3811dcdf87735L,0x75c7d6beL,727,0x5d5770bfd11f5fbbL,820,784129656},{0x6db4616541769502L,-751186835,729,0x4aac5a330db2b2fcL,822,627303724},{0x4490bcdf48ea1d21L,-1006362684,732,0x777a29eb491deb2dL,825,144692500}};
        for (int i = 0; i < ed5datas.length; ++i) {
            ED5_A[i] = new ED5(ed5datas[i]);
        }
    }

    public final long y;
    public final int f;
    public final short dfb;

    public final long oy;
    public final int of;
    public final short ob;

    ED5(long[] data) {
        this.y = data[0];
        this.f = (int) data[1];
        this.dfb = (short) data[2];
        this.oy = data[3];
        this.ob = (short) data[4];
        this.of = (int) data[5];
    }
}