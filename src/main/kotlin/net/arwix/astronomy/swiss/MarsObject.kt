/*
 * Copyright 2017 Vitaliy Sheyanov vit.onix@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.arwix.astronomy.swiss

object MarsObject {
    /*
	First date in file = 1228000.50 / last 2817104.5
	Number of records = 397276.0
	Days per record = 4.0
	      Julian Years      Lon    Lat    Rad
	 -1349.9 to  -1000.0:   0.42   0.18   0.25
	 -1000.0 to   -500.0:   0.45   0.14   0.21
	  -500.0 to      0.0:   0.37   0.10   0.20
	     0.0 to    500.0:   0.33   0.09   0.22
	   500.0 to   1000.0:   0.48   0.07   0.22
	  1000.0 to   1500.0:   0.40   0.07   0.19
	  1500.0 to   2000.0:   0.36   0.11   0.19
	  2000.0 to   2500.0:   0.38   0.14   0.20
	  2500.0 to   3000.0:   0.45   0.15   0.24
	  3000.0 to   3000.8:  0.182  0.125  0.087
	*/
    val tabl = doubleArrayOf(43471.66140, 21291.11063, 2033.37848, 6890507597.78366, 1279543.73631, 317.74183, 730.69258, -15.26502, 277.56960, -62.96711, 20.96285, 1.01857, -2.19395, 3.75708, 3.65854, 0.01049, 1.09183, -0.00605, -0.04769, 0.41839, 0.10091, 0.03887, 0.11666, -0.03301, 0.02664, 0.38777, -0.56974, 0.02974, -0.15041, 0.02179, -0.00808, 0.08594, 0.09773, -0.00902, -0.04597, 0.00762, -0.03858, -0.00139, 0.01562, 0.02019, 0.01878, -0.01244, 0.00795, 0.00815, 0.03501, -0.00335, -0.02970, -0.00518, -0.01763, 0.17257, 0.14698, -0.14417, 0.26028, 0.00062, -0.00180, 13.35262, 39.38771, -15.49558, 22.00150, -7.71321, -4.20035, 0.62074, -1.42376, 0.07043, -0.06670, 0.16960, -0.06859, 0.07787, 0.01845, -0.01608, -0.00914, 5.60438, -3.44436, 5.88876, 6.77238, -5.29704, 3.48944, 0.01291, 0.01280, -0.53532, 0.86584, 0.79604, 0.31635, -3.92977, -0.94829, -0.74254, -1.37947, 0.17871, -0.12477, 0.00171, 0.11537, 0.02281, -0.03922, -0.00165, 0.02965, 1.59773, 1.24565, -0.35802, 1.37272, -0.44811, -0.08611, 3.04184, -3.39729, 8.86270, 6.65967, -9.10580, 10.66103, 0.02015, -0.00902, -0.01166, -0.23957, -0.12128, -0.04640, -0.07114, 0.14053, -0.04966, -0.01665, 0.28411, -0.37754, -1.26265, 1.01377, 3.70433, -0.21025, -0.00972, 0.00350, 0.00997, 0.00450, -2.15305, 3.18147, -1.81957, -0.02321, -0.02560, -0.35188, 0.00003, -0.01110, 0.00244, -0.05083, -0.00216, -0.02026, 0.05179, 0.04188, 5.92031, -1.61316, 3.72001, 6.98783, -4.17690, 2.61250, 0.04157, 2.76453, -1.34043, 0.74586, -0.20258, -0.30467, 0.00733, 0.00376, 1.72800, 0.76593, 1.26577, -2.02682, -1.14637, -0.91894, -0.00002, 0.00036, 2.54213, 0.89533, -0.04166, 2.36838, -0.97069, 0.05486, 0.46927, 0.04500, 0.23388, 0.35005, 1.61402, 2.30209, -0.99859, 1.63349, -0.51490, -0.26112, 0.27848, -0.26100, -0.07645, -0.22001, 0.92901, 1.12627, -0.39829, 0.77120, -0.23716, -0.11245, -0.02387, 0.03960, -0.00802, 0.02179, 2.86448, 1.00246, -0.14647, 2.80278, -1.14143, 0.05177, 1.68671, -1.23451, 3.16285, 0.70070, 0.25817, 3.17416, 0.07447, -0.08116, -0.03029, -0.02795, 0.00816, 0.01023, 0.00685, -0.01075, -0.34268, 0.03680, -0.05488, -0.07430, -0.00041, -0.02968, 3.13228, -0.83209, 1.95765, 3.78394, -2.26196, 1.38520, -0.00401, -0.01397, 1.01604, -0.99485, 0.62465, 0.22431, -0.05076, 0.12025, 4.35229, -5.04483, 14.87533, 9.00826, -10.37595, 19.26596, 0.40352, 0.19895, 0.09463, -0.10774, -0.17809, -0.08979, -0.00796, -0.04313, 0.01520, -0.03538, 1.53301, -1.75553, 4.87236, 3.23662, -3.62305, 6.42351, -0.00439, -0.01305, 0.17194, -0.64003, 0.26609, 0.06600, 0.01767, -0.00251, -0.08871, -0.15523, 0.01201, -0.03408, -0.29126, -0.07093, -0.00998, -0.07876, 1.05932, -25.38650, -0.29354, 0.04179, -0.01726, 0.07473, -0.07607, -0.08859, 0.00842, -0.02359, 0.47858, -0.39809, 1.25061, 0.87017, -0.82453, 1.56864, -0.00463, 0.02385, -0.29070, 8.56535, -0.12495, 0.06580, -0.03395, -0.02465, -1.06759, 0.47004, -0.40281, -0.23957, 0.03572, -0.07012, 0.00571, -0.00731, 0.18601, -1.34068, 0.03798, -0.00532, 0.00448, -0.01147, 1.41208, -0.00668, 0.25883, 1.23788, -0.57774, 0.09166, -2.49664, -0.25235, -0.53582, -0.80126, 0.10827, -0.08861, -0.03577, 0.06825, -0.00143, 0.04633, 0.01586, -0.01056, -0.02106, 0.03804, -0.00088, -0.03458, -0.00033, -0.01079, 0.05821, -0.02445, 0.00602, 0.00721, -0.00315, -0.01021, -0.65454, 1.08478, -0.44593, -0.21492, -1.35004, 4.47299, -4.19170, 3.51236, 1946.04629, 13960.88247, 576.24572, 8023.81797, 2402.48512, -753.87007, -6376.99217, -10278.88014, -25743.89874, 15506.87748, 15609.59853, 35173.63133, -3.70370, 6.29538, -4.84183, -0.76942, -0.02465, -0.03840, 0.00565, -0.06071, 0.01174, 0.00253, -0.00230, 0.05252, -0.02813, 0.01359, 0.23208, 0.03393, 0.01734, 0.04838, -0.46340, -0.18941, 0.25428, -0.56925, 0.05213, 0.24704, 0.12922, -0.01531, 0.06885, -0.08510, 0.01853, -0.00390, 0.01196, -0.30530, 0.13117, -0.03533, 1.79597, -0.42743, 0.98545, 2.13503, -1.32942, 0.68005, -0.01226, 0.00571, 0.31081, 0.34932, 0.34531, -0.32947, -0.00548, 0.00186, -0.00157, -0.00065, 0.30877, -0.03864, 0.04921, 0.06693, 0.01761, -0.04119, 1.28318, 0.38546, 0.06462, 1.18337, -0.48698, 0.07086, 0.26031, -0.22813, 0.10272, 0.04737, -0.04506, -0.38581, -0.16624, -0.04588, 0.00992, 0.00722, -0.21041, 0.20560, -0.09267, -0.03438, 0.32264, -0.07383, 0.09553, -0.38730, 0.17109, -0.01342, -0.02336, -0.01286, 0.00230, 0.04626, 0.01176, 0.01868, -0.15411, -0.32799, 0.22083, -0.14077, 1.98392, 1.68058, -0.02526, -0.13164, -0.04447, -0.00153, 0.01277, 0.00553, -0.26035, -0.11362, 0.14672, -0.32242, 0.16686, -0.69957, 0.40091, -0.06721, 0.00837, 0.09635, -0.08545, 0.25178, -0.22486, 16.03256, 0.34130, -0.06313, 0.01469, -0.09012, -0.00744, -0.02510, -0.08492, -0.13733, -0.07620, -0.15329, 0.13716, -0.03769, 2.01176, -1.35991, -1.04319, -2.97226, -0.01433, 0.61219, -0.55522, 0.38579, 0.31831, 0.81843, -0.04583, -0.14585, -0.10218, 0.16039, -0.06552, -0.01802, 0.06480, -0.06641, 0.01672, -0.00287, 0.00308, 0.09982, -0.05679, -0.00249, -0.36034, 0.52385, -0.29759, 0.59539, -3.59641, -1.02499, -547.53774, 734.11470, 441.86760, -626.68255, -2255.81376, -1309.01028, -2025.69590, 2774.69901, 1711.21478, 1509.99797, -0.99274, 0.61858, -0.47634, -0.33034, 0.00261, 0.01183, -0.00038, 0.11687, 0.00994, -0.01122, 0.03482, -0.01942, -0.11557, 0.38237, -0.17826, 0.00830, 0.01193, -0.05469, 0.01557, 0.01747, 0.02730, -0.01182, -0.11284, 0.12939, -0.05621, -0.01615, 0.04258, 0.01058, -0.01723, 0.00963, 0.20666, 0.11742, 0.07830, -0.02922, -0.10659, -0.05407, 0.07254, -0.13005, -0.02365, 0.24583, 0.31915, 1.27060, 0.00009, -0.21541, -0.55324, -0.45999, -1.45885, 0.86530, 0.85932, 1.92999, -0.00755, -0.00715, -0.02004, -0.00788, 0.01539, 0.00837, 0.27652, -0.50297, -0.26703, -0.28159, 0.03950, 0.07182, -0.07177, 0.14140, 0.07693, 0.07564, -0.01316, -0.01259, 0.01529, 0.07773, -90.74225, -378.15784, -510.30190, -52.35396, -89.15267, 415.56828, 181.52119, 54.01570, -0.01093, -0.05931, -0.01344, -0.02390, 0.01432, -0.02470, -0.01509, -0.01346, 0.03352, 0.02248, 0.02588, -0.00948, 0.03610, 0.17238, 0.02909, -0.04065, 0.00155, -0.07025, -0.09508, 0.14487, 0.12441, 0.16451, 0.00001, -0.00005, -0.00982, -0.01895, -0.16968, 0.36565, 0.20234, 0.17789, -0.04519, -0.00588, 0.01268, 0.00107, -56.32137, -58.22145, -80.55270, 28.14532, 11.43301, 52.05752, 17.79480, -2.61997, -0.00005, -0.02629, 0.01080, -0.00390, 0.00744, 0.03132, 0.01156, -0.01621, 0.02162, 0.02552, 0.00075, -0.02497, 0.02495, 0.00830, 0.03230, 0.00103, -14.84965, -4.50200, -9.73043, 9.40426, 4.08054, 5.38571, 1.53731, -1.01288, 0.21076, 1.74227, 0.79760, 0.39583, 0.09879, -0.16736, -0.00723, -0.01536)
    val tabb = doubleArrayOf(-364.49380, -47.17612, -554.97858, -430.63121, 596.44312, -3.94434, -7.43169, -0.06665, -2.23987, 0.10366, -0.05567, -0.01463, 0.01908, -0.02611, -0.00350, -0.01057, -0.00610, -0.00015, 0.00002, 0.00010, 0.00033, 0.00007, -0.00000, -0.00010, -0.00004, 0.00012, 0.00002, -0.00014, -0.00048, -0.00003, -0.00007, 0.00008, -0.00005, -0.00043, -0.00003, -0.00010, -0.00004, 0.00001, 0.00001, -0.00003, -0.00003, 0.00004, 0.00007, -0.00041, 0.00031, 0.00076, 0.00062, 0.00001, -0.00002, 0.00035, 0.00053, 0.00026, 0.00019, 0.00020, 0.00010, 0.02936, 0.09624, -0.01153, 0.01386, 0.00551, -0.00690, 0.00196, 0.00148, -0.00408, -0.00673, -0.00067, -0.00152, -0.00014, -0.00005, 0.00000, 0.00005, -0.00116, 0.00276, -0.00391, 0.00983, -0.01327, -0.01986, -0.00003, 0.00001, 0.01104, 0.00631, -0.01364, 0.01152, -0.00439, 0.01103, -0.00546, 0.00181, -0.00039, -0.00083, 0.00007, 0.00002, -0.00010, -0.00008, 0.00005, 0.00002, -0.00584, 0.00512, -0.00722, -0.00174, 0.00101, -0.00316, -0.02229, -0.02797, -0.10718, 0.05741, 0.11403, 0.10033, 0.00036, -0.00022, 0.00787, 0.01191, 0.01756, -0.02121, -0.00169, -0.00364, 0.00070, -0.00051, 0.01850, -0.06836, 0.21471, 0.00162, -0.29165, 0.16799, -0.00002, 0.00011, -0.00075, -0.00077, -0.00675, -0.00814, 0.00029, -0.00599, 0.00107, 0.00013, 0.00010, -0.00002, 0.00005, 0.00020, 0.00355, 0.00306, -0.00013, -0.00061, -0.02950, -0.00847, 0.01037, -0.04783, 0.04237, 0.11662, -0.00331, 0.00207, -0.00107, -0.00264, 0.00072, -0.00023, -0.00151, 0.00146, -0.12847, 0.02294, 0.03611, 0.19705, 0.16855, -0.28279, -0.00000, -0.00002, -0.00525, -0.03619, 0.05048, -0.00481, -0.00745, 0.04618, 0.00286, 0.00443, 0.00521, -0.00351, 0.00200, 0.00474, -0.00149, 0.00031, -0.00003, 0.00029, 0.00686, 0.02467, 0.04275, -0.02223, 0.02282, -0.04228, 0.03312, 0.01847, -0.01253, 0.01601, 0.00076, 0.00091, 0.00045, 0.00035, 0.00658, 0.01586, -0.00310, 0.00628, -0.00045, 0.00316, -0.01602, -0.00340, -0.01744, 0.04907, 0.06426, 0.02275, -0.00217, -0.00377, -0.00091, 0.00037, 0.00040, -0.00003, -0.00017, -0.00027, 0.00366, 0.02693, -0.00934, 0.00386, 0.00616, -0.00037, 0.02028, 0.02120, -0.01768, 0.02421, 0.00102, 0.00877, 0.00012, 0.00030, -0.00019, -0.02165, 0.01245, -0.00742, 0.00172, 0.00320, -0.17117, -0.12908, -0.43134, 0.15617, 0.21216, 0.56432, 0.01139, -0.00937, -0.00058, -0.00337, -0.00999, 0.01862, -0.00621, -0.00080, -0.00025, -0.00140, 0.09250, 0.01173, -0.03549, 0.14651, -0.01784, 0.00945, 0.00000, -0.00006, -0.00500, 0.00086, 0.01079, -0.00002, -0.00012, -0.00029, -0.02661, 0.00140, -0.00524, -0.00460, -0.00352, -0.00563, -0.00277, -0.00052, -0.10171, -0.02001, 0.00045, 0.00265, -0.00082, 0.00160, -0.00302, -0.00434, -0.00022, -0.00134, 0.03285, 0.02964, -0.05612, -0.00668, -0.01821, 0.06590, 0.00039, 0.00061, -0.13531, -0.03831, 0.02553, 0.02130, -0.00336, 0.00468, -0.04522, -0.05540, 0.00129, -0.01767, 0.00181, 0.00031, -0.00011, -0.00034, -0.00146, 0.01101, -0.00030, 0.00240, -0.00039, 0.00072, -0.01954, -0.03822, 0.09682, -0.04541, -0.01567, 0.09617, -0.03371, 0.33028, -0.12102, 0.05874, -0.00990, -0.02236, 0.00109, 0.00158, -0.00482, 0.00019, -0.00036, 0.00004, 0.00024, 0.00201, 0.00017, 0.00011, -0.00012, 0.00002, -0.00323, -0.01062, -0.00130, 0.00091, 0.00056, -0.00017, 0.00774, 0.00601, 0.02550, 0.01700, -0.84327, 0.77533, -0.71414, -0.50643, -473.30877, -1504.79179, -458.52274, -865.82237, -417.34994, -681.03976, 765.50697, -1653.67165, 4427.33176, 710.53895, -5016.39367, 4280.60361, 0.33957, 0.38390, -0.38631, 0.81193, 0.00154, -0.00043, 0.01103, -0.00017, -0.00046, 0.00221, 0.00059, 0.00014, 0.00160, 0.00475, 0.06191, -0.13289, 0.02884, -0.00566, -0.01572, 0.23780, -0.05140, -0.03228, -0.00716, -0.00978, -0.01048, 0.01317, -0.01267, -0.01198, 0.00037, -0.00330, -0.02305, 0.00355, -0.00121, -0.00496, -0.04369, -0.01343, 0.05347, -0.12433, 0.02090, 0.17683, 0.00028, -0.00490, -0.02778, -0.05587, -0.01658, 0.05655, 0.00204, -0.00092, 0.00020, 0.00014, -0.00603, -0.03829, 0.00778, -0.00588, -0.00266, 0.00097, -0.02158, -0.07742, 0.09306, -0.01827, -0.01048, 0.07885, -0.02485, -0.02505, 0.00471, -0.01026, 0.06663, 0.01110, 0.00469, -0.05347, -0.00016, -0.00013, 0.02622, 0.02273, -0.01009, 0.01391, -0.01042, -0.00444, -0.04293, -0.00767, -0.00154, -0.01739, 0.00353, -0.00763, -0.00060, 0.00010, -0.00053, -0.00146, -0.05317, 0.05760, -0.01801, -0.02099, -0.02611, -0.01836, -0.00256, 0.00812, -0.00145, 0.00054, -0.00008, 0.00015, -0.04087, 0.08860, -0.05385, -0.02134, 0.02771, 0.02441, -0.00234, 0.01571, -0.00260, 0.00097, 0.10151, 0.49378, -0.28555, 0.11428, -0.00286, 0.01224, 0.00160, 0.00069, 0.00000, -0.00040, -0.13286, 0.00448, 0.01225, -0.00568, 0.00341, 0.00224, -0.23483, -0.07859, 0.30733, -0.21548, -0.02608, 0.00756, 0.09789, 0.02878, -0.11968, 0.08981, 0.02046, -0.00888, 0.02955, 0.01486, -0.00981, 0.01542, -0.01674, -0.01540, 0.00019, -0.00449, -0.02140, 0.00638, 0.00112, -0.00730, -0.08571, 0.13811, -0.16951, -0.02917, -0.03931, -0.32643, -68.64541, -81.00521, -47.97737, 15.75290, 181.76392, -36.00647, -48.32098, -259.02226, -265.57466, 554.05904, 0.09017, 0.18803, -0.12459, 0.10852, 0.00211, 0.00002, 0.00304, -0.00370, 0.00174, 0.00279, 0.00139, 0.00095, 0.04881, 0.00262, -0.01020, 0.03762, 0.00987, 0.00612, 0.00054, -0.00036, 0.00009, -0.00094, 0.02279, 0.01785, -0.00778, 0.01263, 0.00040, -0.00112, -0.00452, -0.00662, 0.00483, -0.00030, -0.00054, -0.00205, -0.00052, -0.00362, -0.00215, -0.00247, 0.02893, -0.01965, -0.00004, 0.04114, -0.00284, -0.00103, 0.01827, -0.07822, 0.18010, 0.04805, -0.21702, 0.18808, 0.00095, -0.00132, -0.01488, 0.00746, 0.00198, 0.00190, 0.01032, 0.03392, 0.04318, -0.07332, -0.01004, 0.00787, -0.00308, -0.01177, -0.01431, 0.02659, 0.00273, -0.00374, -0.02545, 0.00644, 28.68376, 13.74978, 29.60401, -47.98255, -65.91944, -18.48404, -1.73580, 64.67487, -0.02492, 0.00104, -0.00829, -0.00134, 0.00077, 0.00005, -0.00513, 0.00403, 0.00071, -0.00047, -0.00023, -0.00063, 0.00120, 0.00370, -0.00038, -0.00037, 0.00080, -0.00018, 0.00866, 0.00156, -0.01064, 0.02131, 0.00000, -0.00001, 0.00038, -0.00068, -0.00909, -0.02187, -0.02599, 0.05507, -0.00022, -0.01468, 0.00032, 0.00500, 9.86233, -2.85314, -2.25791, -13.83444, -12.38794, 3.79861, 2.76343, 6.63505, 0.00066, 0.00007, -0.00016, -0.00039, 0.00014, 0.00059, -0.00031, -0.00024, -0.00168, 0.00259, 0.00007, -0.00005, -0.00052, 0.00558, 0.00110, 0.01037, 1.59224, -2.37284, -2.00023, -2.28280, -1.49571, 1.48293, 0.60041, 0.56376, -0.54386, 0.03568, -0.10392, 0.31005, 0.09104, 0.03015, 0.00826, -0.00524)
    val tabr = doubleArrayOf(-816.07287, -381.41365, -33.69436, 177.22955, 0.18630, -8.29605, -11.15519, -0.57407, -3.53642, 0.16663, -0.06334, -0.03056, 0.02767, -0.04161, 0.03917, -0.02425, 0.00204, -0.00034, 0.00023, 0.00058, -0.00111, 0.00039, -0.00015, 0.00006, -0.00023, 0.00237, 0.00191, 0.00154, -0.00029, 0.00009, 0.00011, -0.00041, 0.00037, -0.00010, -0.00064, 0.00015, -0.00005, 0.00012, -0.00003, -0.00034, 0.00026, 0.00011, -0.00007, -0.00158, 0.00087, 0.00278, 0.00137, 0.00024, -0.00020, 0.00530, -0.00448, 0.00780, 0.00408, 0.00062, 0.00035, -1.35261, 0.79891, -0.81597, -0.43774, 0.14713, -0.27415, 0.05298, 0.02230, -0.02089, -0.01070, -0.00374, 0.00342, -0.00142, 0.00270, -0.00039, 0.00063, 0.16024, 0.27088, -0.32127, 0.27467, -0.16615, -0.24460, -0.00073, 0.00032, -0.05710, -0.05265, -0.06025, 0.05120, -0.05295, 0.23477, -0.08211, 0.04575, -0.00769, -0.01067, -0.00570, 0.00015, -0.00251, -0.00140, -0.00131, -0.00018, -0.12246, 0.15836, -0.13065, -0.03222, 0.00795, -0.04232, -0.36585, -0.31154, 0.68504, -0.96006, 1.19304, 0.88631, 0.00132, 0.00046, 0.13105, 0.04252, 0.05164, -0.06837, -0.01351, -0.01458, 0.00376, -0.00557, 0.28532, -0.17290, -0.53946, -0.79365, -0.95246, 0.74984, 0.00019, 0.00132, -0.00163, -0.00295, -0.40106, -0.26573, -0.00155, -0.22655, 0.04349, -0.00376, 0.00149, -0.00001, 0.00523, 0.00078, 0.01203, 0.00558, -0.00708, 0.00520, -0.36428, -1.28827, 1.50845, -0.83063, 0.58802, 0.89998, -0.55256, 0.01255, -0.15169, -0.26715, 0.06061, -0.04122, -0.00397, 0.00534, -0.52576, 1.22031, 1.44098, 0.92406, 0.67214, -0.85486, -0.00010, 0.00001, 0.28820, -0.84198, 0.78291, 0.00251, 0.02398, 0.32093, -0.02331, 0.10109, -0.07555, 0.03557, -0.61580, 0.43399, -0.43779, -0.26390, 0.06885, -0.13803, 0.17694, 0.19245, 0.15119, -0.05100, 0.49469, -0.45028, 0.33590, 0.15677, -0.04702, 0.10265, -0.00942, -0.00580, -0.00555, -0.00252, -0.32933, 0.92539, -0.91004, -0.04490, -0.01812, -0.37121, 0.34695, 0.50855, -0.24721, 0.86063, -0.84747, 0.01983, 0.01948, 0.02039, 0.00748, -0.00727, -0.00271, 0.00220, 0.00309, 0.00196, 0.02030, 0.17201, -0.03716, 0.02801, 0.01871, 0.00002, 0.31736, 1.17319, -1.42245, 0.73416, -0.52302, -0.85056, 0.00522, -0.00126, 0.33571, 0.34594, -0.07709, 0.21114, -0.04066, -0.01742, 1.72228, 1.46934, -3.06437, 5.06723, -6.53800, -3.55839, -0.06933, 0.13815, 0.03684, 0.03284, -0.04841, 0.09571, -0.02350, 0.00418, 0.01302, 0.00579, 0.73408, 0.64718, -1.37437, 2.04816, -2.70756, -1.52808, 0.00523, -0.00166, 0.25915, 0.06900, -0.02758, 0.10707, 0.00062, 0.00744, -0.08117, 0.04840, -0.01806, -0.00637, 0.03034, -0.12414, 0.03419, -0.00388, 10.92603, 0.48169, -0.01753, -0.12853, -0.03207, -0.00801, 0.03904, -0.03326, 0.01033, 0.00366, 0.17249, 0.20846, -0.38157, 0.54639, -0.68518, -0.36121, -0.01043, -0.00186, -3.33843, -0.16353, 0.03462, 0.06669, -0.01305, 0.01803, -0.22703, -0.52219, 0.11709, -0.19628, 0.03410, 0.01741, 0.00338, 0.00265, 0.63213, 0.08944, 0.00236, 0.01829, 0.00546, 0.00218, 0.00073, -0.72570, 0.63698, -0.13340, 0.04698, 0.29716, -0.13126, 1.27705, -0.40980, 0.27400, -0.04525, -0.05529, -0.03249, -0.01696, -0.02314, -0.00076, 0.00510, 0.00764, -0.01847, -0.01021, 0.01688, -0.00044, 0.00531, -0.00016, -0.01219, -0.02903, -0.00361, 0.00299, 0.00504, -0.00153, -0.53625, -0.32460, 0.10642, -0.22070, -2.21651, -0.66036, -1.74652, -2.08198, -6810.78679, 967.02869, -3915.97140, 291.65905, 372.99563, 1196.01966, 5108.01033, -3172.64698, -7685.78246, -12789.43898, -17474.50562, 7757.84703, 3.13224, 1.84743, -0.38257, 2.40590, 0.01860, -0.01217, 0.03004, 0.00278, -0.00125, 0.00579, -0.02673, -0.00112, 0.00662, 0.01374, -0.02729, 0.13109, -0.02836, 0.00877, 0.12171, -0.27475, 0.34765, 0.15882, -0.12548, 0.02603, 0.00710, 0.06538, -0.04039, -0.03257, -0.00186, -0.00880, 0.16643, 0.00707, 0.01918, 0.07156, -0.20459, -0.85107, 1.01832, -0.47158, 0.32582, 0.63002, -0.00282, -0.00711, -0.19695, 0.15053, 0.15676, 0.17847, 0.00071, 0.00286, -0.00039, 0.00083, 0.02009, 0.17859, -0.03894, 0.02805, 0.02379, 0.00752, 0.17529, -0.57783, 0.53257, -0.02829, 0.03211, 0.21777, 0.13813, 0.16305, -0.02996, 0.06303, 0.21058, -0.02659, 0.02596, -0.08808, -0.00389, 0.00586, 0.08986, 0.09204, -0.01480, 0.04031, 0.06115, 0.18366, 0.25636, 0.06905, 0.00719, 0.11391, 0.00636, -0.01113, -0.02808, 0.00150, -0.01219, 0.00832, 0.28626, -0.09573, 0.10481, 0.16559, -0.94578, 1.26394, 0.08846, -0.01623, 0.00082, -0.02640, -0.00347, 0.00798, 0.12873, -0.21248, 0.27999, 0.14348, 0.44082, 0.10453, 0.04362, 0.25332, -0.06077, 0.00555, -0.06947, -0.05511, -10.08703, -0.10614, 0.04059, 0.21355, 0.05632, 0.00871, 0.01599, -0.00531, 0.36835, -0.03530, 0.09519, -0.04961, 0.02568, 0.08613, 0.57033, 0.84599, 1.27123, -0.41266, -0.36937, -0.00655, -0.16547, -0.24000, -0.35213, 0.13345, 0.05870, -0.01524, 0.06419, 0.04136, -0.00681, 0.02606, -0.02519, -0.02732, -0.00105, -0.00677, -0.03891, 0.00106, 0.00087, -0.02256, -0.20834, -0.14624, -0.23178, -0.11786, 0.32479, -1.41222, -303.74549, -202.79324, 260.20290, 184.84320, 536.68016, -881.56427, -1125.64824, -791.09928, -596.61162, 659.35664, 0.24561, 0.39519, -0.12601, 0.18709, -0.00700, 0.00136, 0.30750, 0.00009, 0.00443, 0.00384, 0.01170, 0.02078, 0.15043, 0.04802, 0.00386, 0.06942, 0.02107, 0.00495, -0.01067, 0.00951, 0.00937, 0.01996, 0.04922, 0.04337, -0.00583, 0.02110, -0.00691, 0.02793, -0.00364, -0.00682, -0.09143, 0.15369, 0.02043, 0.05451, 0.04053, -0.08179, 0.09645, 0.05330, -0.10149, -0.01594, -0.96773, 0.13660, 0.17326, 0.00013, 0.20990, -0.23184, -0.38407, -0.64733, -0.84754, 0.38889, 0.00310, -0.00340, 0.00970, -0.00788, -0.01111, 0.00677, 0.18147, 0.09968, 0.10170, -0.09233, -0.03165, 0.01790, -0.04727, -0.02364, -0.02546, 0.02451, 0.00442, -0.00426, -0.02540, 0.00471, 130.42585, -31.30051, 17.99957, -174.75585, -142.96798, -27.89752, -19.42122, 59.14872, -0.01899, 0.00388, -0.01265, 0.00694, 0.01966, 0.01140, -0.00439, 0.00503, -0.01867, 0.02826, 0.00752, 0.02012, -0.14734, 0.01909, 0.03312, 0.02327, 0.05843, 0.00061, -0.06958, -0.05798, -0.09174, 0.06242, 0.00003, 0.00001, 0.00670, -0.00305, -0.13637, -0.06058, -0.06372, 0.07257, 0.00209, -0.01369, -0.00044, 0.00355, 17.90079, -17.48270, -8.77915, -24.54483, -15.67123, 3.62668, 0.52038, 5.13220, 0.02574, 0.00003, 0.00339, 0.00919, -0.02778, 0.00464, 0.01429, 0.01003, -0.01661, 0.01327, 0.02216, 0.00034, -0.00389, 0.01076, -0.00035, 0.00983, 1.23731, -4.18017, -2.61932, -2.66346, -1.45540, 1.10310, 0.23322, 0.40775, -0.43623, 0.06212, -0.09900, 0.19456, 0.03639, 0.02566, 0.00309, -0.00116)
    val args = intArrayOf(0, 4, 3, 4, 3, -8, 4, 3, 5, 2, 3, 5, 2, -6, 3, -4, 4, 0, 2, 2, 5, -5, 6, 1, 3, 12, 3, -24, 4, 9, 5, 0, 3, 2, 2, 1, 3, -8, 4, 1, 3, 11, 3, -21, 4, 2, 5, 0, 3, 3, 2, -7, 3, 4, 4, 0, 3, 7, 3, -13, 4, -1, 5, 1, 3, 1, 3, -2, 4, 2, 6, 0, 3, 1, 2, -8, 3, 12, 4, 1, 3, 1, 4, -8, 5, 4, 6, 0, 3, 1, 4, -7, 5, 2, 6, 0, 3, 1, 4, -9, 5, 7, 6, 0, 1, 1, 7, 0, 2, 1, 5, -2, 6, 0, 3, 1, 3, -2, 4, 1, 5, 0, 3, 3, 3, -6, 4, 2, 5, 1, 3, 12, 3, -23, 4, 3, 5, 0, 2, 8, 3, -15, 4, 3, 2, 1, 4, -6, 5, 2, 3, 2, 2, -7, 3, 7, 4, 0, 2, 1, 2, -3, 4, 2, 2, 2, 5, -4, 6, 0, 1, 1, 6, 1, 2, 9, 3, -17, 4, 2, 3, 2, 3, -4, 4, 2, 5, 0, 3, 2, 3, -4, 4, 1, 5, 0, 2, 1, 5, -1, 6, 0, 2, 2, 2, -6, 4, 2, 2, 1, 3, -2, 4, 2, 2, 2, 5, -3, 6, 0, 1, 2, 6, 1, 2, 3, 5, -5, 6, 1, 1, 1, 5, 2, 3, 4, 3, -8, 4, 2, 5, 0, 2, 1, 5, -5, 6, 0, 2, 7, 3, -13, 4, 2, 2, 3, 2, -9, 4, 0, 2, 2, 5, -2, 6, 0, 1, 3, 6, 0, 2, 1, 4, -5, 5, 0, 2, 2, 3, -4, 4, 2, 2, 6, 3, -11, 4, 2, 2, 4, 5, -5, 6, 0, 1, 2, 5, 2, 3, 1, 4, -3, 5, -3, 6, 0, 2, 3, 3, -6, 4, 2, 2, 1, 4, -4, 5, 1, 2, 5, 3, -9, 4, 2, 1, 3, 5, 1, 2, 4, 3, -8, 4, 2, 3, 1, 4, -4, 5, 2, 6, 0, 3, 1, 4, -1, 5, -5, 6, 0, 2, 4, 3, -7, 4, 2, 2, 1, 4, -3, 5, 2, 3, 1, 4, -5, 5, 5, 6, 1, 3, 1, 4, -4, 5, 3, 6, 0, 3, 1, 4, -3, 5, 1, 6, 0, 2, 5, 3, -10, 4, 1, 1, 4, 5, 0, 2, 3, 3, -5, 4, 2, 3, 1, 4, -3, 5, 2, 6, 0, 2, 1, 4, -5, 6, 2, 2, 1, 4, -2, 5, 2, 3, 1, 4, -4, 5, 5, 6, 1, 2, 6, 3, -12, 4, 1, 2, 1, 4, -4, 6, 0, 2, 2, 3, -3, 4, 2, 2, 10, 3, -18, 4, 0, 2, 1, 4, -3, 6, 1, 3, 1, 4, -2, 5, 2, 6, 0, 2, 7, 3, -14, 4, 1, 3, 1, 4, 1, 5, -5, 6, 1, 2, 1, 4, -1, 5, 0, 3, 1, 4, -3, 5, 5, 6, 1, 3, 1, 4, 2, 5, -7, 6, 1, 2, 1, 4, -2, 6, 2, 3, 1, 4, -2, 5, 3, 6, 0, 2, 1, 3, -1, 4, 0, 2, 2, 2, -7, 4, 1, 2, 9, 3, -16, 4, 2, 2, 1, 4, -3, 7, 0, 2, 1, 4, -1, 6, 0, 3, 1, 4, -2, 5, 4, 6, 1, 2, 1, 2, -4, 4, 2, 2, 8, 3, -16, 4, 2, 2, 1, 4, -2, 7, 0, 3, 3, 3, -5, 4, 2, 5, 0, 3, 1, 4, 1, 5, -3, 6, 0, 2, 1, 4, -2, 8, 0, 2, 1, 4, -1, 7, 0, 2, 1, 4, -1, 8, 0, 3, 3, 2, -7, 3, 3, 4, 0, 3, 2, 2, 1, 3, -7, 4, 0, 3, 1, 4, 1, 6, -3, 7, 0, 3, 1, 4, 2, 5, -5, 6, 1, 3, 4, 3, -7, 4, 3, 5, 1, 1, 1, 4, 5, 3, 4, 3, -9, 4, 3, 5, 1, 3, 1, 4, -2, 5, 5, 6, 0, 3, 3, 2, -7, 3, 5, 4, 0, 3, 1, 3, -1, 4, 2, 6, 0, 3, 1, 4, 1, 5, -2, 6, 0, 3, 3, 3, -7, 4, 2, 5, 0, 2, 8, 3, -14, 4, 1, 2, 1, 2, -2, 4, 1, 2, 1, 4, 1, 6, 1, 2, 9, 3, -18, 4, 1, 2, 2, 2, -5, 4, 1, 2, 1, 3, -3, 4, 2, 2, 1, 4, 2, 6, 0, 2, 1, 4, 1, 5, 1, 3, 4, 3, -9, 4, 2, 5, 1, 2, 7, 3, -12, 4, 1, 2, 2, 4, -5, 5, 0, 2, 2, 3, -5, 4, 2, 2, 6, 3, -10, 4, 1, 2, 1, 4, 2, 5, 1, 3, 2, 4, -5, 5, 2, 6, 0, 2, 3, 3, -7, 4, 1, 2, 2, 4, -4, 5, 0, 2, 5, 3, -8, 4, 1, 2, 1, 4, 3, 5, 0, 3, 2, 4, -4, 5, 2, 6, 0, 3, 2, 4, -1, 5, -5, 6, 0, 2, 4, 3, -6, 4, 1, 2, 2, 4, -3, 5, 0, 3, 2, 4, -5, 5, 5, 6, 1, 3, 2, 4, -4, 5, 3, 6, 0, 2, 3, 3, -4, 4, 1, 2, 2, 4, -5, 6, 2, 2, 2, 4, -2, 5, 1, 3, 2, 4, -4, 5, 5, 6, 1, 2, 2, 4, -4, 6, 0, 2, 2, 3, -2, 4, 0, 2, 2, 4, -3, 6, 1, 2, 2, 4, -1, 5, 1, 2, 2, 4, -2, 6, 0, 1, 1, 3, 1, 2, 2, 4, -1, 6, 0, 2, 1, 2, -5, 4, 1, 2, 8, 3, -17, 4, 1, 3, 2, 4, 2, 5, -5, 6, 1, 3, 4, 3, -6, 4, 3, 5, 1, 3, 10, 3, -17, 4, 3, 6, 0, 1, 2, 4, 4, 3, 4, 3, -10, 4, 3, 5, 1, 2, 8, 3, -13, 4, 0, 2, 1, 2, -1, 4, 0, 2, 2, 4, 1, 6, 0, 2, 2, 2, -4, 4, 0, 2, 1, 3, -4, 4, 1, 2, 2, 4, 1, 5, 0, 2, 7, 3, -11, 4, 0, 2, 3, 4, -5, 5, 0, 2, 2, 3, -6, 4, 1, 2, 6, 3, -9, 4, 0, 2, 2, 4, 2, 5, 0, 2, 3, 4, -4, 5, 0, 2, 5, 3, -7, 4, 0, 2, 4, 3, -5, 4, 1, 2, 3, 4, -3, 5, 1, 2, 3, 3, -3, 4, 0, 2, 3, 4, -2, 5, 2, 3, 3, 4, -4, 5, 5, 6, 0, 2, 2, 3, -1, 4, 0, 2, 3, 4, -3, 6, 0, 2, 3, 4, -1, 5, 1, 2, 3, 4, -2, 6, 0, 2, 1, 3, 1, 4, 1, 2, 3, 4, -1, 6, 0, 3, 4, 3, -5, 4, 3, 5, 0, 1, 3, 4, 3, 3, 4, 3, -11, 4, 3, 5, 0, 1, 1, 2, 0, 2, 2, 2, -3, 4, 0, 2, 1, 3, -5, 4, 0, 2, 4, 4, -5, 5, 0, 2, 6, 3, -8, 4, 0, 2, 4, 4, -4, 5, 0, 2, 5, 3, -6, 4, 0, 2, 4, 3, -4, 4, 0, 2, 4, 4, -3, 5, 1, 3, 6, 3, -8, 4, 2, 5, 0, 2, 3, 3, -2, 4, 0, 2, 4, 4, -2, 5, 1, 2, 4, 4, -1, 5, 0, 2, 1, 3, 2, 4, 0, 1, 4, 4, 3, 2, 2, 2, -2, 4, 0, 2, 7, 3, -9, 4, 0, 2, 5, 4, -5, 5, 0, 2, 6, 3, -7, 4, 0, 2, 5, 4, -4, 5, 0, 2, 5, 3, -5, 4, 0, 2, 5, 4, -3, 5, 0, 2, 5, 4, -2, 5, 0, 1, 5, 4, 3, 1, 6, 4, 2, 1, 7, 4, 0, -1)
    /* Total terms = 201, small = 199 */
    val maxargs = 9
    val max_harmonic = intArrayOf(0, 5, 12, 24, 9, 7, 3, 2, 0)
    val max_power_of_t = 5
    val distance = 1.5303348827100001
    val timescale = 3652500.0
    val trunclvl = 1.0
}