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

package net.arwix.astronomy.ephem

import net.arwix.astronomy.core.ARCSEC_TO_RAD
import net.arwix.astronomy.core.DEG_TO_RAD
import net.arwix.astronomy.core.vector.RectangularVector
import net.arwix.astronomy.core.vector.Vector
import net.arwix.astronomy.core.vector.VectorType
import java.lang.Math.*


object Nutation {

    /**
     * Array to hold sines of multiple angles
     */
    private val ss = Array(5) { DoubleArray(8) }

    /**
     * Array to hold cosines of multiple angles
     */
    private val cc = Array(5) { DoubleArray(8) }


    /**
     * Returns the nutation in longitude and obliquity following Petter Duffet's
     * book Astronomy with your Personal Computer.
     * @param jd Julian day in dynamical time.
     * *
     * @return Nutation in longitude and obliquity in radians.
     */
    fun getFastNutation(t: Double): DoubleArray {

        var DP: Double
        var DOSR: Double

        val T2 = t * t
        var aq = 100.0021358 * t
        var b = 360.0 * (aq - floor(aq))
        val L1 = 279.6967 + .000303 * T2 + b
        val L2 = 2.0 * L1 * DEG_TO_RAD
        aq = 1336.855231 * t
        b = 360.0 * (aq - floor(aq))
        val D1 = 270.4342 - .001133 * T2 + b
        val D2 = 2.0 * D1 * DEG_TO_RAD
        aq = 99.99736056 * t
        b = 360.0 * (aq - floor(aq))
        val M1 = (358.4758 - .00015 * T2 + b) * DEG_TO_RAD
        aq = 1325.552359 * t
        b = 360.0 * (aq - floor(aq))
        val M2 = (296.1046 + .009192 * T2 + b) * DEG_TO_RAD
        aq = 5.372616667 * t
        b = 360.0 * (aq - floor(aq))
        val N1 = (259.1833 + .002078 * T2 - b) * DEG_TO_RAD
        val N2 = 2.0 * N1 // * Constant.DEG_TO_RAD // A bug in Peter Duffett's code !?

        DP = (-17.2327 - .01737 * t) * sin(N1)
        DP += (-1.2729 - .00013 * t) * sin(L2) + .2088 * sin(N2)
        DP += -.2037 * sin(D2) + (.1261 - .00031 * t) * sin(M1)
        DP += .0675 * sin(M2) - (.0497 - .00012 * t) * sin(L2 + M1)
        DP += -.0342 * sin(D2 - N1) - .0261 * sin(D2 + M2)
        DP += .0214 * sin(L2 - M1) - .0149 * sin(L2 - D2 + M2)
        DP += .0124 * sin(L2 - N1) + .0114 * sin(D2 - M2)

        DOSR = (9.21 + .00091 * t) * cos(N1)
        DOSR += (.5522 - .00029 * t) * cos(L2) - .0904 * cos(N2)
        DOSR += .0884 * cos(D2) + .0216 * cos(L2 + M1)
        DOSR += .0183 * cos(D2 - N1) + .0113 * cos(D2 + M2)
        DOSR += -.0093 * cos(L2 - M1) - .0066 * cos(L2 - N1)

        DP *= ARCSEC_TO_RAD
        DOSR *= ARCSEC_TO_RAD

        return doubleArrayOf(DP, DOSR)
    }

    /**
     * Nutates equatorial coordinates from mean dynamical equator and equinox of date to true
     * equator and equinox, or the opposite. See AA Explanatory Supplement, page 114-115.
     * For dates between 1900 and 2100 and the flag to prefer precision in the ephemeris
     * object set to false, the approximate code by Peter Duffet for nutation is used.
     * @param jd_tt Julian day in TT.
     * @param eph Ephemeris properties.
     * @param in Input equatorial coordinates.
     * @param meanToTrue True to nutate from mean to true position, false for true to mean.
     *
     * @return Output equatorial coordinates.
     */
    fun nutateInEquatorialCoordinates(t: Double, isFastCals: Boolean,
                                      inVector: Vector, meanToTrue: Boolean): Vector {

        val nut = if (!isFastCals || t > 1) Nutation.calcNutation(t) else getFastNutation(t)
        val oblm = Obliquity.meanObliquity(t)
        val oblt = oblm + nut[1]
        val dpsi = nut[0]

        val cobm = Math.cos(oblm)
        val sobm = Math.sin(oblm)
        val cobt = Math.cos(oblt)
        val sobt = Math.sin(oblt)
        val cpsi = Math.cos(dpsi)
        val spsi = Math.sin(dpsi)

        // Compute elements of nutation matrix
        val xx = cpsi
        val yx = -spsi * cobm
        val zx = -spsi * sobm
        val xy = spsi * cobt
        val yy = cpsi * cobm * cobt + sobm * sobt
        val zy = cpsi * sobm * cobt - cobm * sobt
        val xz = spsi * sobt
        val yz = cpsi * cobm * sobt - sobm * cobt
        val zz = cpsi * sobm * sobt + cobm * cobt

        val vector = inVector.getVectorOfType(VectorType.RECTANGULAR) as RectangularVector

        val out = DoubleArray(3)
        if (meanToTrue) {
            out[0] = xx * vector.x + yx * vector.y + zx * vector.z
            out[1] = xy * vector.x + yy * vector.y + zy * vector.z
            out[2] = xz * vector.x + yz * vector.y + zz * vector.z
//            if (out.size == 6) {
//                out[3] = xx * `in`[3] + yx * `in`[4] + zx * `in`[5]
//                out[4] = xy * `in`[3] + yy * `in`[4] + zy * `in`[5]
//                out[5] = xz * `in`[3] + yz * `in`[4] + zz * `in`[5]
//            }
        } else {
            out[0] = xx * vector.x + xy * vector.y + xz * vector.z
            out[1] = yx * vector.x + yy * vector.y + yz * vector.z
            out[2] = zx * vector.x + zy * vector.y + zz * vector.z
//            if (out.size == 6) {
//                out[3] = xx * `in`[3] + yx * `in`[4] + zx * `in`[5]
//                out[4] = xy * `in`[3] + yy * `in`[4] + zy * `in`[5]
//                out[5] = xz * `in`[3] + yz * `in`[4] + zz * `in`[5]
//            }
        }

        return RectangularVector(out)
    }

    /**
     * Calculate nutation in longitude and obliquity. Results are saved in
     * [DataBase], using as identifier 'Nutation' for the array
     * containing the values.

     * @param T Julian centuries from J2000 epoch in dynamical time.
     * *
     * @param eph Ephemeris properties including if EOP correction should be
     * * applied and the ephem method selection: IAU2006/2009, IAU2000, or any other
     * * (for IAU1980 nutation).
     * *
     * @return The two values calculated for nutation in longitude and in obliquity.
     * *
     * @throws JPARSECException If an error occurs accesing EOP files when required.
     */
    fun calcNutation(T: Double): DoubleArray {
        /*
		// This code is to ensure EOP is called before calculating nutation. It is not required since EOP is called by
		// TimeScale.getJD, and that method is called always in any ephemeris calculation before nutation
		try {
			double jd = Constant.J2000 + T * Constant.JULIAN_DAYS_PER_CENTURY;
			double UT12TT = TimeScale.getTTminusUT1(new TimeElement(jd, SCALE.BARYCENTRIC_DYNAMICAL_TIME), new ObserverElement()) / Constant.SECONDS_PER_DAY;
			EphemerisElement eph = new EphemerisElement();
			eph.ephemMethod = type;
			eph.frame = FRAME.ICRF;
			EarthOrientationParameters.obtainEOP(jd - UT12TT, eph);
		} catch (Exception exc) {}
		*/

        var nutationInLongitude = 0.0
        var nutationInObliquity = 0.0
//        when (type) {
//            IAU_1976, LASKAR_1986, SIMON_1994, WILLIAMS_1994, JPL_DE4xx -> {
//                var n = calcNutation_IAU1980(T, eph)
//                nutationInLongitude = n[0]
//                nutationInObliquity = n[1]
//            }
//            IAU_2006, IAU_2009 -> {
        val n = calcNutation_IAU2000(T)
        nutationInLongitude = n[0]
        nutationInObliquity = n[1]

        // Apply precession adjustments, see Wallace & Capitaine, 2006, Eqs.5
        nutationInLongitude += nutationInLongitude * (0.4697E-6 - 2.7774E-6 * T)
        nutationInObliquity -= nutationInObliquity * (2.7774E-6 * T)
//            }
//            else // IAU_2000
//            -> {
//                n = calcNutation_IAU2000(T, eph)
//                nutationInLongitude = n[0]
//                nutationInObliquity = n[1]
//            }
//        }

        //      lastCalc = doubleArrayOf(nutationInLongitude, nutationInObliquity, T, type.ordinal())

        /*		DataBase.addData("Nutation", new double[] {
				nutationInLongitude, nutationInObliquity, T, type.ordinal()
		}, true);
*/
        return doubleArrayOf(nutationInLongitude, nutationInObliquity)
    }


    /**
     * Nutation, IAU 2000A model (MHB2000 luni-solar and planetary nutation with
     * free core nutation omitted) plus optional support for free core nutation.
     * This method is based on SOFA (Standards of Astronomy) software library.
     * Results are set in fields [Nutation.nutationInLongitude] and
     * [Nutation.nutationInObliquity].
     * <P>
     * The nutation components in longitude and obliquity are with respect to
     * the equinox and ecliptic of date. The obliquity at J2000 is assumed to be
     * the Lieske et al. (1977) value of 84381.448 arcsec.
     * Both the luni-solar and planetary nutations are included. The latter are
     * due to direct planetary nutations and the perturbations of the lunar and
     * terrestrial orbits.
     *
     * The routine computes the MHB2000 nutation series with the associated
     * corrections for planetary nutations. It is an implementation of the
     * nutation part of the IAU 2000A precession- nutation model, formally
     * adopted by the IAU General Assembly in 2000, namely MHB2000 (Mathews et
     * al. 2002), but with the free core nutation (FCN) omitted.
     * The full MHB2000 model also contains contributions to the nutations in
     * longitude and obliquity due to the free-excitation of the
     * free-core-nutation during the period 1979-2000. These FCN terms, which
     * are time-dependent and unpredictable, are included in the present routine
     * if [EarthOrientationParameters.obtainEOP] is previously called
     * (note this should always happen since this method is needed by
     * [TimeScale.getJD]).
     *
     * With the FCN corrections included, the present
     * routine delivers a pole which is at current epochs accurate to a few
     * hundred microarcseconds. The omission of FCN introduces further errors of
     * about that size.
     *
     * The present routine provides classical nutation. The MHB2000 algorithm,
     * from which it is adapted, deals also with (i) the offsets between the
     * GCRS and mean poles and (ii) the adjustments in longitude and obliquity
     * due to the changed precession rates. These additional functions, namely
     * frame bias and precession adjustments, are applied independently in this
     * package. Bias correction is made automatically in this library when
     * selecting ICRS frame. The precession adjustments are applied in the
     * precession methods from Capitaine and IAU2000 models.
     *
     * References:
     * Mathews, P.M., Herring, T.A., Buffet, B.A., "Modeling of nutation and
     * precession New nutation series for nonrigid Earth and insights into the
     * Earth's interior", J.Geophys.Res., 107, B4, 2002. The MHB2000 code itself
     * was obtained on 9th September 2002 from <A target="_blank" href = "ftp://maia.usno.navy.mil/conv2000/chapter5/IAU2000A">
     * ftp://maia.usno.navy.mil/conv2000/chapter5/IAU2000A</A>.
     * Souchay, J., Loysel, B., Kinoshita, H., Folgueira, M., A&A Supp. Ser.
     * 135, 111 (1999).
     * Wallace, P.T., "Software for Implementing the IAU 2000 Resolutions", in
     * IERS Workshop 5.1 (2002).
     * Chapront, J., Chapront-Touze, M. & Francou, G., Astron.Astrophys., 387,
     * 700 (2002).
     * Lieske, J.H., Lederle, T., Fricke, W. & Morando, B., "Expressions for the
     * precession quantities based upon the IAU (1976) System of Astronomical
     * Constants", Astron.Astrophys., 58, 1-16 (1977).
     * Simon, J.-L., Bretagnon, P., Chapront, J., Chapront-Touze, M., Francou,
     * G., Laskar, J., A&A282, 663-683 (1994).
     * This revision: 2005 August 24.
     *
     * Copyright (C) 2005 IAU SOFA Review Board.
     * @param T Time in Julian centuries from J2000.
     * @throws JPARSECException If an error occurs.
    </P> */
    private fun calcNutation_IAU2000(T: Double): DoubleArray {
        // * Initialize the nutation values.
        var DP = 0.0
        var DE = 0.0
        var nutationInLongitude = 0.0
        var nutationInObliquity = 0.0

        /*
		 * ------------------- LUNI-SOLAR NUTATION -------------------
		 */

        /*
		 * Fundamental (Delaunay) arguments from Simon et al. (1994)
		 */

        // * Mean anomaly of the Moon.
        val EL = (485868.249036 + T * (1717915923.2178 + T * (31.8792 + T * (0.051635 + T * -0.00024470)))) * ARCSEC_TO_RAD

        // * Mean anomaly of the Sun.
        val ELP = (1287104.79305 + T * (129596581.0481 + T * (-0.5532 + T * (0.000136 + T * -0.00001149)))) * ARCSEC_TO_RAD

        // * Mean argument of the latitude of the Moon.
        val F = (335779.526232 + T * (1739527262.8478 + T * (-12.7512 + T * (-0.001037 + T * 0.00000417)))) * ARCSEC_TO_RAD

        // * Mean elongation of the Moon from the Sun.
        val D = (1072260.70369 + T * (1602961601.2090 + T * (-6.3706 + T * (0.006593 + T * -0.00003169)))) * ARCSEC_TO_RAD

        // * Mean longitude of the ascending node of the Moon.
        val OM = (450160.398036 + T * (-6962890.5431 + T * (7.4722 + T * (0.007702 + T * -0.00005939)))) * ARCSEC_TO_RAD

        // * Summation of luni-solar nutation series (in reverse order).
        for (I in 677 downTo 0) {
            val NALS_index = I * 5 - 1

            // * Argument and functions.
            val ARG = IAU2000_NALS.NALS[NALS_index + 1] * EL + IAU2000_NALS.NALS[NALS_index + 2] * ELP + IAU2000_NALS.NALS[NALS_index + 3] * F + IAU2000_NALS.NALS[NALS_index + 4] * D + IAU2000_NALS.NALS[NALS_index + 5] * OM
            val SARG = Math.sin(ARG)
            val CARG = Math.cos(ARG)

            val CLS_index = I * 6 - 1

            // * Term.
            DP += (IAU2000_CLS.CLS[CLS_index + 1] + IAU2000_CLS.CLS[CLS_index + 2] * T) * SARG + IAU2000_CLS.CLS[CLS_index + 3] * CARG
            DE += (IAU2000_CLS.CLS[CLS_index + 4] + IAU2000_CLS.CLS[CLS_index + 5] * T) * CARG + IAU2000_CLS.CLS[CLS_index + 6] * SARG

        }

        // * Convert from 0.1 microarcsec units to radians.
        nutationInLongitude = DP * ARCSEC_TO_RAD / 1.0e7
        nutationInObliquity = DE * ARCSEC_TO_RAD / 1.0e7

        /*
		 * ------------------ PLANETARY NUTATION ------------------
		 */

        /*
		 * n.b. The MHB2000 code computes the luni-solar and planetary nutation
		 * in different routines, using slightly different Delaunay arguments in
		 * the two cases. This behaviour is faithfully reproduced here. Use of
		 * the Simon et al. expressions for both cases leads to negligible
		 * changes, well below 0.1 microarcsecond.
		 */

        // * Mean anomaly of the Moon.
        val AL = 2.35555598 + 8328.6914269554 * T

        // * Mean anomaly of the Sun.
        val ALSU = 6.24006013 + 628.301955 * T

        // * Mean argument of the latitude of the Moon.
        val AF = 1.627905234 + 8433.466158131 * T

        // * Mean elongation of the Moon from the Sun.
        val AD = 5.198466741 + 7771.3771468121 * T

        // * Mean longitude of the ascending node of the Moon.
        val AOM = 2.18243920 - 33.757045 * T

        // * General accumulated precession in longitude.
        val APA = (0.02438175 + 0.00000538691 * T) * T

        // * Planetary longitudes, MercuryObject through Neptune (Souchay et al.
        // 1999).
        val ALME = 4.402608842 + 2608.7903141574 * T
        val ALVE = 3.176146697 + 1021.3285546211 * T
        val ALEA = 1.753470314 + 628.3075849991 * T
        val ALMA = 6.203480913 + 334.0612426700 * T
        val ALJU = 0.599546497 + 52.9690962641 * T
        val ALSA = 0.874016757 + 21.3299104960 * T
        val ALUR = 5.481293871 + 7.4781598567 * T
        val ALNE = 5.321159000 + 3.8127774000 * T

        // * Initialize the nutation values.
        DP = 0.0
        DE = 0.0

        // * Summation of planetary nutation series (in reverse order).
        for (I in 686 downTo 0) {

            val NAPL_index = I * 14 - 1

            // * Argument and functions.
            val ARG = IAU2000_NAPL.NAPL[NAPL_index + 1] * AL + IAU2000_NAPL.NAPL[NAPL_index + 2] * ALSU + IAU2000_NAPL.NAPL[NAPL_index + 3] * AF + IAU2000_NAPL.NAPL[NAPL_index + 4] * AD + IAU2000_NAPL.NAPL[NAPL_index + 5] * AOM + IAU2000_NAPL.NAPL[NAPL_index + 6] * ALME + IAU2000_NAPL.NAPL[NAPL_index + 7] * ALVE + IAU2000_NAPL.NAPL[NAPL_index + 8] * ALEA + IAU2000_NAPL.NAPL[NAPL_index + 9] * ALMA + IAU2000_NAPL.NAPL[NAPL_index + 10] * ALJU + IAU2000_NAPL.NAPL[NAPL_index + 11] * ALSA + IAU2000_NAPL.NAPL[NAPL_index + 12] * ALUR + IAU2000_NAPL.NAPL[NAPL_index + 13] * ALNE + IAU2000_NAPL.NAPL[NAPL_index + 14] * APA
            val SARG = Math.sin(ARG)
            val CARG = Math.cos(ARG)

            val ICPL_index = I * 4 - 1

            // * Term.
            DP = DP + IAU2000_ICPL.ICPL[ICPL_index + 1] * SARG + IAU2000_ICPL.ICPL[ICPL_index + 2] * CARG
            DE = DE + IAU2000_ICPL.ICPL[ICPL_index + 3] * SARG + IAU2000_ICPL.ICPL[ICPL_index + 4] * CARG
        }

        // * Add luni-solar and planetary components converting from 0.1
        // microarcsecond.
        nutationInLongitude += DP * ARCSEC_TO_RAD / 1.0e7
        nutationInObliquity += DE * ARCSEC_TO_RAD / 1.0e7

        return doubleArrayOf(nutationInLongitude, nutationInObliquity)
    }

}

/**
 * Nutation, IAU 2000A model (MHB2000 luni-solar and planetary nutation with
 * free core nutation omitted).
 */
internal object IAU2000_NALS {
    /*
	 * Luni-Solar argument multipliers L L' F D Om
	 */
    var NALS = doubleArrayOf(
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 1, 10 )/
            0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, -2.0, 2.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, -2.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 1.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, -1.0, 2.0, -2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 11, 20 )/
            0.0, 0.0, 2.0, -2.0, 1.0, -1.0, 0.0, 2.0, 0.0, 2.0, -1.0, 0.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 2.0, 2.0, 2.0, 1.0, 0.0, 2.0, 0.0, 1.0, -2.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 21, 30 )/
            0.0, -2.0, 2.0, -2.0, 2.0, -2.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, 1.0, 0.0, 2.0, -2.0, 2.0, -1.0, 0.0, 2.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 2.0, 1.0, 0.0, 2.0, 2.0, -2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 31, 40 )/
            0.0, 0.0, -2.0, 2.0, 0.0, 1.0, 0.0, 0.0, -2.0, 1.0, 0.0, -1.0, 0.0, 0.0, 1.0, -1.0, 0.0, 2.0, 2.0, 1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 2.0, 2.0, -2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 2.0, 0.0, 2.0, 0.0, 0.0, 2.0, 2.0, 1.0, 0.0, -1.0, 2.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 41, 50 )/
            0.0, 0.0, 0.0, 2.0, 1.0, 1.0, 0.0, 2.0, -2.0, 1.0, 2.0, 0.0, 2.0, -2.0, 2.0, -2.0, 0.0, 0.0, 2.0, 1.0, 2.0, 0.0, 2.0, 0.0, 1.0, 0.0, -1.0, 2.0, -2.0, 1.0, 0.0, 0.0, 0.0, -2.0, 1.0, -1.0, -1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 1.0, 0.0, 0.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 51, 60 )/
            0.0, 1.0, 2.0, -2.0, 1.0, 1.0, -1.0, 0.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 2.0, 3.0, 0.0, 2.0, 0.0, 2.0, 0.0, -1.0, 0.0, 2.0, 0.0, 1.0, -1.0, 2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, -1.0, 2.0, 2.0, 2.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, -1.0, 2.0, 2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 61, 70 )/
            -2.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 0.0, 2.0, 2.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 0.0, -1.0, 0.0, 2.0, -2.0, 1.0, 1.0, 0.0, 0.0, 0.0, 2.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 1.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 71, 80 )/
            -1.0, 0.0, 2.0, 4.0, 2.0, -1.0, 1.0, 0.0, 1.0, 1.0, 0.0, -2.0, 2.0, -2.0, 1.0, 1.0, 0.0, 2.0, 2.0, 1.0, -2.0, 0.0, 2.0, 2.0, 2.0, -1.0, 0.0, 0.0, 0.0, 2.0, 1.0, 1.0, 2.0, -2.0, 2.0, -2.0, 0.0, 2.0, 4.0, 2.0, -1.0, 0.0, 4.0, 0.0, 2.0, 2.0, 0.0, 2.0, -2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 81, 90 )/
            2.0, 0.0, 2.0, 2.0, 2.0, 1.0, 0.0, 0.0, 2.0, 1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 2.0, -2.0, 2.0, 0.0, 0.0, 4.0, -2.0, 2.0, 0.0, 1.0, 2.0, 0.0, 1.0, 0.0, 0.0, -2.0, 2.0, 1.0, 0.0, 0.0, 2.0, -2.0, 3.0, -1.0, 0.0, 0.0, 4.0, 0.0, 2.0, 0.0, -2.0, 0.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J= 91,100 )/
            -2.0, 0.0, 0.0, 4.0, 0.0, -1.0, -1.0, 0.0, 2.0, 1.0, -1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 0.0, 1.0, 0.0, -1.0, 2.0, 0.0, 1.0, 0.0, 0.0, 2.0, -1.0, 2.0, 0.0, 0.0, 2.0, 4.0, 2.0, -2.0, -1.0, 0.0, 2.0, 0.0, 1.0, 1.0, 0.0, -2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=101,110 )/
            -1.0, 1.0, 0.0, 2.0, 0.0, -1.0, 1.0, 0.0, 1.0, 2.0, 1.0, -1.0, 0.0, 0.0, 1.0, 1.0, -1.0, 2.0, 2.0, 2.0, -1.0, 1.0, 2.0, 2.0, 2.0, 3.0, 0.0, 2.0, 0.0, 1.0, 0.0, 1.0, -2.0, 2.0, 0.0, -1.0, 0.0, 0.0, -2.0, 1.0, 0.0, 1.0, 2.0, 2.0, 2.0, -1.0, -1.0, 2.0, 2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=111,120 )/
            0.0, -1.0, 0.0, 0.0, 2.0, 1.0, 0.0, 2.0, -4.0, 1.0, -1.0, 0.0, -2.0, 2.0, 0.0, 0.0, -1.0, 2.0, 2.0, 1.0, 2.0, -1.0, 2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 2.0, 1.0, -1.0, 2.0, 0.0, 1.0, -1.0, 1.0, 2.0, 0.0, 2.0, 0.0, 1.0, 0.0, 2.0, 0.0, 0.0, -1.0, -2.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=121,130 )/
            0.0, 3.0, 2.0, -2.0, 2.0, 0.0, 0.0, 0.0, 1.0, 1.0, -1.0, 0.0, 2.0, 2.0, 0.0, 2.0, 1.0, 2.0, 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 0.0, 1.0, 2.0, 0.0, 0.0, 2.0, 0.0, 1.0, 0.0, -2.0, 2.0, 0.0, -1.0, 0.0, 0.0, 2.0, 2.0, 0.0, 1.0, 0.0, 1.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=131,140 )/
            0.0, 1.0, 0.0, -2.0, 1.0, -1.0, 0.0, 2.0, -2.0, 2.0, 0.0, 0.0, 0.0, -1.0, 1.0, -1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 2.0, -1.0, 2.0, 1.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 1.0, 0.0, 2.0, 1.0, 2.0, 0.0, 0.0, 2.0, 1.0, 1.0, 1.0, 0.0, 0.0, -2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=141,150 )/
            -1.0, 0.0, 2.0, 4.0, 1.0, 1.0, 0.0, -2.0, 0.0, 1.0, 1.0, 1.0, 2.0, -2.0, 1.0, 0.0, 0.0, 2.0, 2.0, 0.0, -1.0, 0.0, 2.0, -1.0, 1.0, -2.0, 0.0, 2.0, 2.0, 1.0, 4.0, 0.0, 2.0, 0.0, 2.0, 2.0, -1.0, 0.0, 0.0, 0.0, 2.0, 1.0, 2.0, -2.0, 2.0, 0.0, 1.0, 2.0, 1.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=151,160 )/
            1.0, 0.0, 4.0, -2.0, 2.0, -1.0, -1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 2.0, 1.0, -2.0, 0.0, 2.0, 4.0, 1.0, 2.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 4.0, 1.0, -1.0, 0.0, 4.0, 0.0, 1.0, 2.0, 0.0, 2.0, 2.0, 1.0, 0.0, 0.0, 2.0, -3.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=161,170 )/
            -1.0, -2.0, 0.0, 2.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -4.0, 1.0, 0.0, -1.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 4.0, 1.0, -1.0, -1.0, 2.0, 4.0, 2.0, 1.0, 0.0, 2.0, 4.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=171,180 )/
            -2.0, 2.0, 0.0, 2.0, 0.0, -2.0, -1.0, 2.0, 0.0, 1.0, -2.0, 0.0, 0.0, 2.0, 2.0, -1.0, -1.0, 2.0, 0.0, 2.0, 0.0, 0.0, 4.0, -2.0, 1.0, 3.0, 0.0, 2.0, -2.0, 1.0, -2.0, -1.0, 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, -1.0, 1.0, 0.0, -2.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 4.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=181,190 )/
            -3.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 4.0, 1.0, 3.0, 0.0, 2.0, 2.0, 2.0, -1.0, 1.0, 2.0, -2.0, 1.0, 2.0, 0.0, 0.0, -4.0, 1.0, 0.0, 0.0, 0.0, -2.0, 2.0, 2.0, 0.0, 2.0, -4.0, 1.0, -1.0, 1.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, -1.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=191,200 )/
            0.0, -2.0, 2.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 1.0, 4.0, 0.0, 2.0, -2.0, 2.0, 2.0, 0.0, 0.0, -2.0, 2.0, 0.0, 2.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, -4.0, 1.0, 0.0, 2.0, 2.0, -2.0, 1.0, -3.0, 0.0, 0.0, 4.0, 0.0, -1.0, 1.0, 2.0, 0.0, 1.0, -1.0, -1.0, 0.0, 4.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=201,210 )/
            -1.0, -2.0, 2.0, 2.0, 2.0, -2.0, -1.0, 2.0, 4.0, 2.0, 1.0, -1.0, 2.0, 2.0, 1.0, -2.0, 1.0, 0.0, 2.0, 0.0, -2.0, 1.0, 2.0, 0.0, 1.0, 2.0, 1.0, 0.0, -2.0, 1.0, -3.0, 0.0, 2.0, 0.0, 1.0, -2.0, 0.0, 2.0, -2.0, 1.0, -1.0, 1.0, 0.0, 2.0, 2.0, 0.0, -1.0, 2.0, -1.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=211,220 )/
            -1.0, 0.0, 4.0, -2.0, 2.0, 0.0, -2.0, 2.0, 0.0, 2.0, -1.0, 0.0, 2.0, 1.0, 2.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, 0.0, 3.0, -2.0, 0.0, 4.0, 0.0, 2.0, -1.0, 0.0, -2.0, 0.0, 1.0, -1.0, 1.0, 2.0, 2.0, 1.0, 3.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 2.0, 3.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=221,230 )/
            2.0, -1.0, 2.0, 0.0, 1.0, 0.0, 1.0, 2.0, 2.0, 1.0, 0.0, -1.0, 2.0, 4.0, 2.0, 2.0, -1.0, 2.0, 2.0, 2.0, 0.0, 2.0, -2.0, 2.0, 0.0, -1.0, -1.0, 2.0, -1.0, 1.0, 0.0, -2.0, 0.0, 0.0, 1.0, 1.0, 0.0, 2.0, -4.0, 2.0, 1.0, -1.0, 0.0, -2.0, 1.0, -1.0, -1.0, 2.0, 0.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=231,240 )/
            1.0, -1.0, 2.0, -2.0, 2.0, -2.0, -1.0, 0.0, 4.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, -2.0, -1.0, 2.0, 2.0, 2.0, 0.0, 2.0, 2.0, 0.0, 2.0, 1.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, -1.0, 2.0, 1.0, 0.0, 2.0, 1.0, 1.0, 4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 2.0, 0.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=241,250 )/
            3.0, -1.0, 2.0, 0.0, 2.0, -2.0, 2.0, 0.0, 2.0, 1.0, 1.0, 0.0, 2.0, -3.0, 1.0, 1.0, 1.0, 2.0, -4.0, 1.0, -1.0, -1.0, 2.0, -2.0, 1.0, 0.0, -1.0, 0.0, -1.0, 1.0, 0.0, -1.0, 0.0, -2.0, 1.0, -2.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, -2.0, 2.0, 0.0, -1.0, 0.0, -2.0, 4.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=251,260 )/
            1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, -1.0, 2.0, 0.0, 2.0, 0.0, 1.0, -1.0, 2.0, -2.0, 1.0, 1.0, 2.0, 2.0, -2.0, 2.0, 2.0, -1.0, 2.0, -2.0, 2.0, 1.0, 0.0, 2.0, -1.0, 1.0, 2.0, 1.0, 2.0, -2.0, 1.0, -2.0, 0.0, 0.0, -2.0, 1.0, 1.0, -2.0, 2.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=261,270 )/
            0.0, 1.0, 2.0, 1.0, 1.0, 1.0, 0.0, 4.0, -2.0, 1.0, -2.0, 0.0, 4.0, 2.0, 2.0, 1.0, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 0.0, 4.0, 0.0, 1.0, 0.0, 2.0, 2.0, 0.0, 2.0, 0.0, 2.0, 1.0, 2.0, 3.0, 1.0, 2.0, 0.0, 2.0, 4.0, 0.0, 2.0, 0.0, 1.0, -2.0, -1.0, 2.0, 0.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=271,280 )/
            0.0, 1.0, -2.0, 2.0, 1.0, 1.0, 0.0, -2.0, 1.0, 0.0, 0.0, -1.0, -2.0, 2.0, 1.0, 2.0, -1.0, 0.0, -2.0, 1.0, -1.0, 0.0, 2.0, -1.0, 2.0, 1.0, 0.0, 2.0, -3.0, 2.0, 0.0, 1.0, 2.0, -2.0, 3.0, 0.0, 0.0, 2.0, -3.0, 1.0, -1.0, 0.0, -2.0, 2.0, 1.0, 0.0, 0.0, 2.0, -4.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=281,290 )/
            -2.0, 1.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, -1.0, 1.0, 2.0, 0.0, 2.0, -4.0, 2.0, 0.0, 0.0, 4.0, -4.0, 4.0, 0.0, 0.0, 4.0, -4.0, 2.0, -1.0, -2.0, 0.0, 2.0, 1.0, -2.0, 0.0, 0.0, 3.0, 0.0, 1.0, 0.0, -2.0, 2.0, 1.0, -3.0, 0.0, 2.0, 2.0, 2.0, -3.0, 0.0, 2.0, 2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=291,300 )/
            -2.0, 0.0, 2.0, 2.0, 0.0, 2.0, -1.0, 0.0, 0.0, 1.0, -2.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 4.0, -2.0, 2.0, -1.0, 1.0, 0.0, -2.0, 1.0, 0.0, 0.0, 0.0, -4.0, 1.0, 1.0, -1.0, 0.0, 2.0, 1.0, 1.0, 1.0, 0.0, 2.0, 1.0, -1.0, 2.0, 2.0, 2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=301,310 )/
            3.0, 1.0, 2.0, -2.0, 2.0, 0.0, -1.0, 0.0, 4.0, 0.0, 2.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 4.0, 0.0, 1.0, 2.0, 0.0, 4.0, -2.0, 2.0, -1.0, -1.0, 2.0, 4.0, 1.0, 1.0, 0.0, 0.0, 4.0, 1.0, 1.0, -2.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 3.0, 2.0, -1.0, 1.0, 2.0, 4.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=311,320 )/
            3.0, 0.0, 0.0, 2.0, 0.0, -1.0, 0.0, 4.0, 2.0, 2.0, 1.0, 1.0, 2.0, 2.0, 1.0, -2.0, 0.0, 2.0, 6.0, 2.0, 2.0, 1.0, 2.0, 2.0, 2.0, -1.0, 0.0, 2.0, 6.0, 2.0, 1.0, 0.0, 2.0, 4.0, 1.0, 2.0, 0.0, 2.0, 4.0, 2.0, 1.0, 1.0, -2.0, 1.0, 0.0, -3.0, 1.0, 2.0, 1.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=321,330 )/
            2.0, 0.0, -2.0, 0.0, 2.0, -1.0, 0.0, 0.0, 1.0, 2.0, -4.0, 0.0, 2.0, 2.0, 1.0, -1.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, -2.0, 2.0, 2.0, 1.0, 0.0, 0.0, -1.0, 2.0, 0.0, -1.0, 2.0, -2.0, 3.0, -2.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 4.0, -2.0, -2.0, 0.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=331,340 )/
            -2.0, 0.0, -2.0, 4.0, 0.0, 0.0, -2.0, -2.0, 2.0, 0.0, 1.0, 2.0, 0.0, -2.0, 1.0, 3.0, 0.0, 0.0, -4.0, 1.0, -1.0, 1.0, 2.0, -2.0, 2.0, 1.0, -1.0, 2.0, -4.0, 1.0, 1.0, 1.0, 0.0, -2.0, 2.0, -3.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 2.0, 0.0, 2.0, -2.0, 0.0, 0.0, 1.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=341,350 )/
            0.0, 0.0, -2.0, 1.0, 0.0, -3.0, 0.0, 0.0, 2.0, 1.0, -1.0, -1.0, -2.0, 2.0, 0.0, 0.0, 1.0, 2.0, -4.0, 1.0, 2.0, 1.0, 0.0, -4.0, 1.0, 0.0, 2.0, 0.0, -2.0, 1.0, 1.0, 0.0, 0.0, -3.0, 1.0, -2.0, 0.0, 2.0, -2.0, 2.0, -2.0, -1.0, 0.0, 0.0, 1.0, -4.0, 0.0, 0.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=351,360 )/
            1.0, 1.0, 0.0, -4.0, 1.0, -1.0, 0.0, 2.0, -4.0, 1.0, 0.0, 0.0, 4.0, -4.0, 1.0, 0.0, 3.0, 2.0, -2.0, 2.0, -3.0, -1.0, 0.0, 4.0, 0.0, -3.0, 0.0, 0.0, 4.0, 1.0, 1.0, -1.0, -2.0, 2.0, 0.0, -1.0, -1.0, 0.0, 2.0, 2.0, 1.0, -2.0, 0.0, 0.0, 1.0, 1.0, -1.0, 0.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=361,370 )/
            0.0, 0.0, 0.0, 1.0, 2.0, -1.0, -1.0, 2.0, 0.0, 0.0, 1.0, -2.0, 2.0, -2.0, 2.0, 0.0, -1.0, 2.0, -1.0, 1.0, -1.0, 0.0, 2.0, 0.0, 3.0, 1.0, 1.0, 0.0, 0.0, 2.0, -1.0, 1.0, 2.0, 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, -1.0, 2.0, 2.0, 0.0, 2.0, -1.0, 0.0, 4.0, -2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=371,380 )/
            3.0, 0.0, 2.0, -4.0, 2.0, 1.0, 2.0, 2.0, -2.0, 1.0, 1.0, 0.0, 4.0, -4.0, 2.0, -2.0, -1.0, 0.0, 4.0, 1.0, 0.0, -1.0, 0.0, 2.0, 2.0, -2.0, 1.0, 0.0, 4.0, 0.0, -2.0, -1.0, 2.0, 2.0, 1.0, 2.0, 0.0, -2.0, 2.0, 0.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=381,390 )/
            1.0, -1.0, 2.0, -1.0, 2.0, -2.0, 0.0, 4.0, 0.0, 1.0, 2.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, -1.0, 4.0, -2.0, 2.0, 0.0, 0.0, 4.0, -2.0, 4.0, 0.0, 2.0, 2.0, 0.0, 1.0, -3.0, 0.0, 0.0, 6.0, 0.0, -1.0, -1.0, 0.0, 4.0, 1.0, 1.0, -2.0, 0.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=391,400 )/
            -1.0, 0.0, 0.0, 4.0, 2.0, -1.0, -2.0, 2.0, 2.0, 1.0, -1.0, 0.0, 0.0, -2.0, 2.0, 1.0, 0.0, -2.0, -2.0, 1.0, 0.0, 0.0, -2.0, -2.0, 1.0, -2.0, 0.0, -2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 3.0, 1.0, 0.0, 0.0, 0.0, 3.0, 0.0, -1.0, 1.0, 0.0, 4.0, 0.0, -1.0, -1.0, 2.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=401,410 )/
            -2.0, 0.0, 2.0, 3.0, 2.0, 1.0, 0.0, 0.0, 2.0, 2.0, 0.0, -1.0, 2.0, 1.0, 2.0, 3.0, -1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 1.0, 0.0, 2.0, 0.0, 3.0, 3.0, 1.0, 0.0, 0.0, 0.0, 3.0, -1.0, 2.0, -2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=411,420 )/
            2.0, 0.0, 2.0, -1.0, 1.0, 1.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 4.0, -1.0, 2.0, 1.0, 2.0, 2.0, 0.0, 2.0, -2.0, 0.0, 0.0, 6.0, 0.0, 0.0, -1.0, 0.0, 4.0, 1.0, -2.0, -1.0, 2.0, 4.0, 1.0, 0.0, -2.0, 2.0, 2.0, 1.0, 0.0, -1.0, 2.0, 2.0, 0.0, -1.0, 0.0, 2.0, 3.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=421,430 )/
            -2.0, 1.0, 2.0, 4.0, 2.0, 2.0, 0.0, 0.0, 2.0, 2.0, 2.0, -2.0, 2.0, 0.0, 2.0, -1.0, 1.0, 2.0, 3.0, 2.0, 3.0, 0.0, 2.0, -1.0, 2.0, 4.0, 0.0, 2.0, -2.0, 1.0, -1.0, 0.0, 0.0, 6.0, 0.0, -1.0, -2.0, 2.0, 4.0, 2.0, -3.0, 0.0, 2.0, 6.0, 2.0, -1.0, 0.0, 2.0, 4.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=431,440 )/
            3.0, 0.0, 0.0, 2.0, 1.0, 3.0, -1.0, 2.0, 0.0, 1.0, 3.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 4.0, 0.0, 2.0, 5.0, 0.0, 2.0, -2.0, 2.0, 0.0, -1.0, 2.0, 4.0, 1.0, 2.0, -1.0, 2.0, 2.0, 1.0, 0.0, 1.0, 2.0, 4.0, 2.0, 1.0, -1.0, 2.0, 4.0, 2.0, 3.0, -1.0, 2.0, 2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=441,450 )/
            3.0, 0.0, 2.0, 2.0, 1.0, 5.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 2.0, 6.0, 2.0, 4.0, 0.0, 2.0, 2.0, 2.0, 0.0, -1.0, 1.0, -1.0, 1.0, -1.0, 0.0, 1.0, 0.0, 3.0, 0.0, -2.0, 2.0, -2.0, 3.0, 1.0, 0.0, -1.0, 0.0, 1.0, 2.0, -2.0, 0.0, -2.0, 1.0, -1.0, 0.0, 1.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=451,460 )/
            -1.0, 0.0, 1.0, 0.0, 1.0, -1.0, -1.0, 2.0, -1.0, 2.0, -2.0, 2.0, 0.0, 2.0, 2.0, -1.0, 0.0, 1.0, 0.0, 0.0, -4.0, 1.0, 2.0, 2.0, 2.0, -3.0, 0.0, 2.0, 1.0, 1.0, -2.0, -1.0, 2.0, 0.0, 2.0, 1.0, 0.0, -2.0, 1.0, 1.0, 2.0, -1.0, -2.0, 0.0, 1.0, -4.0, 0.0, 2.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=461,470 )/
            -3.0, 1.0, 0.0, 3.0, 0.0, -1.0, 0.0, -1.0, 2.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 2.0, -3.0, 0.0, 0.0, 3.0, 0.0, -2.0, -1.0, 0.0, 2.0, 2.0, -1.0, 0.0, -2.0, 3.0, 0.0, -4.0, 0.0, 0.0, 4.0, 0.0, 2.0, 1.0, -2.0, 0.0, 1.0, 2.0, -1.0, 0.0, -2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=471,480 )/
            0.0, 0.0, 1.0, -1.0, 0.0, -1.0, 2.0, 0.0, 1.0, 0.0, -2.0, 1.0, 2.0, 0.0, 2.0, 1.0, 1.0, 0.0, -1.0, 1.0, 1.0, 0.0, 1.0, -2.0, 1.0, 0.0, 2.0, 0.0, 0.0, 2.0, 1.0, -1.0, 2.0, -3.0, 1.0, -1.0, 1.0, 2.0, -1.0, 1.0, -2.0, 0.0, 4.0, -2.0, 2.0, -2.0, 0.0, 4.0, -2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=481,490 )/
            -2.0, -2.0, 0.0, 2.0, 1.0, -2.0, 0.0, -2.0, 4.0, 0.0, 1.0, 2.0, 2.0, -4.0, 1.0, 1.0, 1.0, 2.0, -4.0, 2.0, -1.0, 2.0, 2.0, -2.0, 1.0, 2.0, 0.0, 0.0, -3.0, 1.0, -1.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, -2.0, 0.0, -1.0, -1.0, 2.0, -2.0, 2.0, -1.0, 1.0, 0.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=491,500 )/
            0.0, 0.0, 0.0, -1.0, 2.0, -2.0, 1.0, 0.0, 1.0, 0.0, 1.0, -2.0, 0.0, -2.0, 1.0, 1.0, 0.0, -2.0, 0.0, 2.0, -3.0, 1.0, 0.0, 2.0, 0.0, -1.0, 1.0, -2.0, 2.0, 0.0, -1.0, -1.0, 0.0, 0.0, 2.0, -3.0, 0.0, 0.0, 2.0, 0.0, -3.0, -1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, -6.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=501,510 )/
            0.0, 1.0, 2.0, -4.0, 2.0, 2.0, 0.0, 0.0, -4.0, 2.0, -2.0, 1.0, 2.0, -2.0, 1.0, 0.0, -1.0, 2.0, -4.0, 1.0, 0.0, 1.0, 0.0, -2.0, 2.0, -1.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, -2.0, -2.0, 1.0, -4.0, 0.0, 2.0, 0.0, 1.0, -1.0, -1.0, 0.0, -1.0, 1.0, 0.0, 0.0, -2.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=511,520 )/
            -3.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, -2.0, 1.0, 0.0, -2.0, 0.0, -2.0, 2.0, 1.0, 0.0, 0.0, -4.0, 2.0, 0.0, -2.0, -1.0, -2.0, 2.0, 0.0, 1.0, 0.0, 2.0, -6.0, 1.0, -1.0, 0.0, 2.0, -4.0, 2.0, 1.0, 0.0, 0.0, -4.0, 2.0, 2.0, 1.0, 2.0, -4.0, 2.0, 2.0, 1.0, 2.0, -4.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=521,530 )/
            0.0, 1.0, 4.0, -4.0, 4.0, 0.0, 1.0, 4.0, -4.0, 2.0, -1.0, -1.0, -2.0, 4.0, 0.0, -1.0, -3.0, 0.0, 2.0, 0.0, -1.0, 0.0, -2.0, 4.0, 1.0, -2.0, -1.0, 0.0, 3.0, 0.0, 0.0, 0.0, -2.0, 3.0, 0.0, -2.0, 0.0, 0.0, 3.0, 1.0, 0.0, -1.0, 0.0, 1.0, 0.0, -3.0, 0.0, 2.0, 2.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=531,540 )/
            1.0, 1.0, -2.0, 2.0, 0.0, -1.0, 1.0, 0.0, 2.0, 2.0, 1.0, -2.0, 2.0, -2.0, 1.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 2.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, 2.0, -2.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 0.0, -1.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=541,550 )/
            3.0, 0.0, 0.0, -2.0, 1.0, 1.0, 0.0, 2.0, -2.0, 3.0, 1.0, 2.0, 0.0, 0.0, 1.0, 2.0, 0.0, 2.0, -3.0, 2.0, -1.0, 1.0, 4.0, -2.0, 2.0, -2.0, -2.0, 0.0, 4.0, 0.0, 0.0, -3.0, 0.0, 2.0, 0.0, 0.0, 0.0, -2.0, 4.0, 0.0, -1.0, -1.0, 0.0, 3.0, 0.0, -2.0, 0.0, 0.0, 4.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=551,560 )/
            -1.0, 0.0, 0.0, 3.0, 1.0, 2.0, -2.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 2.0, 0.0, 1.0, -1.0, 0.0, 1.0, 2.0, 1.0, -1.0, 1.0, 0.0, 3.0, 0.0, -1.0, -1.0, 2.0, 1.0, 2.0, 0.0, -1.0, 2.0, 0.0, 0.0, -2.0, 1.0, 2.0, 2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=561,570 )/
            2.0, -2.0, 2.0, -2.0, 2.0, 1.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, -1.0, 2.0, -2.0, 1.0, 0.0, -1.0, 4.0, -2.0, 1.0, 0.0, 0.0, 4.0, -2.0, 3.0, 0.0, 1.0, 4.0, -2.0, 1.0, 4.0, 0.0, 2.0, -4.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=571,580 )/
            2.0, 2.0, 2.0, -2.0, 2.0, 2.0, 0.0, 4.0, -4.0, 2.0, -1.0, -2.0, 0.0, 4.0, 0.0, -1.0, -3.0, 2.0, 2.0, 2.0, -3.0, 0.0, 2.0, 4.0, 2.0, -3.0, 0.0, 2.0, -2.0, 1.0, -1.0, -1.0, 0.0, -2.0, 1.0, -3.0, 0.0, 0.0, 0.0, 2.0, -3.0, 0.0, -2.0, 2.0, 0.0, 0.0, 1.0, 0.0, -4.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=581,590 )/
            -2.0, 1.0, 0.0, -2.0, 1.0, -4.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, -4.0, 1.0, -3.0, 0.0, 0.0, -2.0, 1.0, 0.0, 0.0, 0.0, 3.0, 2.0, -1.0, 1.0, 0.0, 4.0, 1.0, 1.0, -2.0, 2.0, 0.0, 1.0, 0.0, 1.0, 0.0, 3.0, 0.0, -1.0, 0.0, 2.0, 2.0, 3.0, 0.0, 0.0, 2.0, 2.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=591,600 )/
            -2.0, 0.0, 2.0, 2.0, 2.0, -1.0, 1.0, 2.0, 2.0, 0.0, 3.0, 0.0, 0.0, 0.0, 2.0, 2.0, 1.0, 0.0, 1.0, 0.0, 2.0, -1.0, 2.0, -1.0, 2.0, 0.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 3.0, 0.0, 3.0, 0.0, 0.0, 3.0, 0.0, 2.0, -1.0, 2.0, 2.0, 2.0, 1.0, -1.0, 0.0, 4.0, 0.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=601,610 )/
            1.0, 2.0, 2.0, 0.0, 1.0, 3.0, 1.0, 2.0, -2.0, 1.0, 1.0, 1.0, 4.0, -2.0, 2.0, -2.0, -1.0, 0.0, 6.0, 0.0, 0.0, -2.0, 0.0, 4.0, 0.0, -2.0, 0.0, 0.0, 6.0, 1.0, -2.0, -2.0, 2.0, 4.0, 2.0, 0.0, -3.0, 2.0, 2.0, 2.0, 0.0, 0.0, 0.0, 4.0, 2.0, -1.0, -1.0, 2.0, 3.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=611,620 )/
            -2.0, 0.0, 2.0, 4.0, 0.0, 2.0, -1.0, 0.0, 2.0, 1.0, 1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 1.0, 0.0, 4.0, 1.0, 0.0, 1.0, 0.0, 4.0, 0.0, 1.0, -1.0, 2.0, 1.0, 2.0, 0.0, 0.0, 2.0, 2.0, 3.0, 1.0, 0.0, 2.0, 2.0, 2.0, -1.0, 0.0, 2.0, 2.0, 2.0, -2.0, 0.0, 4.0, 2.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=621,630 )/
            2.0, 1.0, 0.0, 2.0, 1.0, 2.0, 1.0, 0.0, 2.0, 0.0, 2.0, -1.0, 2.0, 0.0, 0.0, 1.0, 0.0, 2.0, 1.0, 0.0, 0.0, 1.0, 2.0, 2.0, 0.0, 2.0, 0.0, 2.0, 0.0, 3.0, 3.0, 0.0, 2.0, 0.0, 2.0, 1.0, 0.0, 2.0, 0.0, 2.0, 1.0, 0.0, 3.0, 0.0, 3.0, 1.0, 1.0, 2.0, 1.0, 1.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=631,640 )/
            0.0, 2.0, 2.0, 2.0, 2.0, 2.0, 1.0, 2.0, 0.0, 0.0, 2.0, 0.0, 4.0, -2.0, 1.0, 4.0, 1.0, 2.0, -2.0, 2.0, -1.0, -1.0, 0.0, 6.0, 0.0, -3.0, -1.0, 2.0, 6.0, 2.0, -1.0, 0.0, 0.0, 6.0, 1.0, -3.0, 0.0, 2.0, 6.0, 1.0, 1.0, -1.0, 0.0, 4.0, 1.0, 1.0, -1.0, 0.0, 4.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=641,650 )/
            -2.0, 0.0, 2.0, 5.0, 2.0, 1.0, -2.0, 2.0, 2.0, 1.0, 3.0, -1.0, 0.0, 2.0, 0.0, 1.0, -1.0, 2.0, 2.0, 0.0, 0.0, 0.0, 2.0, 3.0, 1.0, -1.0, 1.0, 2.0, 4.0, 1.0, 0.0, 1.0, 2.0, 3.0, 2.0, -1.0, 0.0, 4.0, 2.0, 1.0, 2.0, 0.0, 2.0, 1.0, 1.0, 5.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=651,660 )/
            2.0, 1.0, 2.0, 1.0, 2.0, 1.0, 0.0, 4.0, 0.0, 1.0, 3.0, 1.0, 2.0, 0.0, 1.0, 3.0, 0.0, 4.0, -2.0, 2.0, -2.0, -1.0, 2.0, 6.0, 2.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -2.0, 2.0, 4.0, 2.0, -2.0, 0.0, 2.0, 6.0, 1.0, 2.0, 0.0, 0.0, 4.0, 1.0, 2.0, 0.0, 0.0, 4.0, 0.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=661,670 )/
            2.0, -2.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 4.0, 0.0, 1.0, 0.0, 2.0, 3.0, 2.0, 4.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, 2.0, 0.0, 0.0, 0.0, 4.0, 2.0, 2.0, 4.0, -1.0, 2.0, 0.0, 2.0, 3.0, 0.0, 2.0, 1.0, 2.0, 2.0, 1.0, 2.0, 2.0, 1.0, 4.0, 1.0, 2.0, 0.0, 2.0,
            // DATA ( ( NALS(I,J)/ I=1,5 )/ J=671,678 )/
            -1.0, -1.0, 2.0, 6.0, 2.0, -1.0, 0.0, 2.0, 6.0, 1.0, 1.0, -1.0, 2.0, 4.0, 1.0, 1.0, 1.0, 2.0, 4.0, 2.0, 3.0, 1.0, 2.0, 2.0, 2.0, 5.0, 0.0, 2.0, 0.0, 1.0, 2.0, -1.0, 2.0, 4.0, 2.0, 2.0, 0.0, 2.0, 4.0, 1.0)
}

internal object IAU2000_CLS {
    /*
	 * Luni-Solar nutation coefficients, unit 1e-7 arcsec longitude (sin, t*sin,
	 * cos) / obliquity (cos, t*cos, sin)
	 */

    var CLS = doubleArrayOf(// DATA ( ( CLS(I,J) / I=1,6 ) / J= 1, 10 ) /
            -172064161.0, -174666.0, 33386.0, 92052331.0, 9086.0, 15377.0, -13170906.0, -1675.0, -13696.0, 5730336.0, -3015.0, -4587.0, -2276413.0, -234.0, 2796.0, 978459.0, -485.0, 1374.0, 2074554.0, 207.0, -698.0, -897492.0, 470.0, -291.0, 1475877.0, -3633.0, 11817.0, 73871.0, -184.0, -1924.0, -516821.0, 1226.0, -524.0, 224386.0, -677.0, -174.0, 711159.0, 73.0, -872.0, -6750.0, 0.0, 358.0, -387298.0, -367.0, 380.0, 200728.0, 18.0, 318.0, -301461.0, -36.0, 816.0, 129025.0, -63.0, 367.0, 215829.0, -494.0, 111.0, -95929.0, 299.0, 132.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 11, 20 ) /
            128227.0, 137.0, 181.0, -68982.0, -9.0, 39.0, 123457.0, 11.0, 19.0, -53311.0, 32.0, -4.0, 156994.0, 10.0, -168.0, -1235.0, 0.0, 82.0, 63110.0, 63.0, 27.0, -33228.0, 0.0, -9.0, -57976.0, -63.0, -189.0, 31429.0, 0.0, -75.0, -59641.0, -11.0, 149.0, 25543.0, -11.0, 66.0, -51613.0, -42.0, 129.0, 26366.0, 0.0, 78.0, 45893.0, 50.0, 31.0, -24236.0, -10.0, 20.0, 63384.0, 11.0, -150.0, -1220.0, 0.0, 29.0, -38571.0, -1.0, 158.0, 16452.0, -11.0, 68.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 21, 30 ) /
            32481.0, 0.0, 0.0, -13870.0, 0.0, 0.0, -47722.0, 0.0, -18.0, 477.0, 0.0, -25.0, -31046.0, -1.0, 131.0, 13238.0, -11.0, 59.0, 28593.0, 0.0, -1.0, -12338.0, 10.0, -3.0, 20441.0, 21.0, 10.0, -10758.0, 0.0, -3.0, 29243.0, 0.0, -74.0, -609.0, 0.0, 13.0, 25887.0, 0.0, -66.0, -550.0, 0.0, 11.0, -14053.0, -25.0, 79.0, 8551.0, -2.0, -45.0, 15164.0, 10.0, 11.0, -8001.0, 0.0, -1.0, -15794.0, 72.0, -16.0, 6850.0, -42.0, -5.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 31, 40 ) /
            21783.0, 0.0, 13.0, -167.0, 0.0, 13.0, -12873.0, -10.0, -37.0, 6953.0, 0.0, -14.0, -12654.0, 11.0, 63.0, 6415.0, 0.0, 26.0, -10204.0, 0.0, 25.0, 5222.0, 0.0, 15.0, 16707.0, -85.0, -10.0, 168.0, -1.0, 10.0, -7691.0, 0.0, 44.0, 3268.0, 0.0, 19.0, -11024.0, 0.0, -14.0, 104.0, 0.0, 2.0, 7566.0, -21.0, -11.0, -3250.0, 0.0, -5.0, -6637.0, -11.0, 25.0, 3353.0, 0.0, 14.0, -7141.0, 21.0, 8.0, 3070.0, 0.0, 4.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 41, 50 ) /
            -6302.0, -11.0, 2.0, 3272.0, 0.0, 4.0, 5800.0, 10.0, 2.0, -3045.0, 0.0, -1.0, 6443.0, 0.0, -7.0, -2768.0, 0.0, -4.0, -5774.0, -11.0, -15.0, 3041.0, 0.0, -5.0, -5350.0, 0.0, 21.0, 2695.0, 0.0, 12.0, -4752.0, -11.0, -3.0, 2719.0, 0.0, -3.0, -4940.0, -11.0, -21.0, 2720.0, 0.0, -9.0, 7350.0, 0.0, -8.0, -51.0, 0.0, 4.0, 4065.0, 0.0, 6.0, -2206.0, 0.0, 1.0, 6579.0, 0.0, -24.0, -199.0, 0.0, 2.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 51, 60 ) /
            3579.0, 0.0, 5.0, -1900.0, 0.0, 1.0, 4725.0, 0.0, -6.0, -41.0, 0.0, 3.0, -3075.0, 0.0, -2.0, 1313.0, 0.0, -1.0, -2904.0, 0.0, 15.0, 1233.0, 0.0, 7.0, 4348.0, 0.0, -10.0, -81.0, 0.0, 2.0, -2878.0, 0.0, 8.0, 1232.0, 0.0, 4.0, -4230.0, 0.0, 5.0, -20.0, 0.0, -2.0, -2819.0, 0.0, 7.0, 1207.0, 0.0, 3.0, -4056.0, 0.0, 5.0, 40.0, 0.0, -2.0, -2647.0, 0.0, 11.0, 1129.0, 0.0, 5.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 61, 70 ) /
            -2294.0, 0.0, -10.0, 1266.0, 0.0, -4.0, 2481.0, 0.0, -7.0, -1062.0, 0.0, -3.0, 2179.0, 0.0, -2.0, -1129.0, 0.0, -2.0, 3276.0, 0.0, 1.0, -9.0, 0.0, 0.0, -3389.0, 0.0, 5.0, 35.0, 0.0, -2.0, 3339.0, 0.0, -13.0, -107.0, 0.0, 1.0, -1987.0, 0.0, -6.0, 1073.0, 0.0, -2.0, -1981.0, 0.0, 0.0, 854.0, 0.0, 0.0, 4026.0, 0.0, -353.0, -553.0, 0.0, -139.0, 1660.0, 0.0, -5.0, -710.0, 0.0, -2.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 71, 80 ) /
            -1521.0, 0.0, 9.0, 647.0, 0.0, 4.0, 1314.0, 0.0, 0.0, -700.0, 0.0, 0.0, -1283.0, 0.0, 0.0, 672.0, 0.0, 0.0, -1331.0, 0.0, 8.0, 663.0, 0.0, 4.0, 1383.0, 0.0, -2.0, -594.0, 0.0, -2.0, 1405.0, 0.0, 4.0, -610.0, 0.0, 2.0, 1290.0, 0.0, 0.0, -556.0, 0.0, 0.0, -1214.0, 0.0, 5.0, 518.0, 0.0, 2.0, 1146.0, 0.0, -3.0, -490.0, 0.0, -1.0, 1019.0, 0.0, -1.0, -527.0, 0.0, -1.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 81, 90 ) /
            -1100.0, 0.0, 9.0, 465.0, 0.0, 4.0, -970.0, 0.0, 2.0, 496.0, 0.0, 1.0, 1575.0, 0.0, -6.0, -50.0, 0.0, 0.0, 934.0, 0.0, -3.0, -399.0, 0.0, -1.0, 922.0, 0.0, -1.0, -395.0, 0.0, -1.0, 815.0, 0.0, -1.0, -422.0, 0.0, -1.0, 834.0, 0.0, 2.0, -440.0, 0.0, 1.0, 1248.0, 0.0, 0.0, -170.0, 0.0, 1.0, 1338.0, 0.0, -5.0, -39.0, 0.0, 0.0, 716.0, 0.0, -2.0, -389.0, 0.0, -1.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J= 91,100 ) /
            1282.0, 0.0, -3.0, -23.0, 0.0, 1.0, 742.0, 0.0, 1.0, -391.0, 0.0, 0.0, 1020.0, 0.0, -25.0, -495.0, 0.0, -10.0, 715.0, 0.0, -4.0, -326.0, 0.0, 2.0, -666.0, 0.0, -3.0, 369.0, 0.0, -1.0, -667.0, 0.0, 1.0, 346.0, 0.0, 1.0, -704.0, 0.0, 0.0, 304.0, 0.0, 0.0, -694.0, 0.0, 5.0, 294.0, 0.0, 2.0, -1014.0, 0.0, -1.0, 4.0, 0.0, -1.0, -585.0, 0.0, -2.0, 316.0, 0.0, -1.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=101,110 ) /
            -949.0, 0.0, 1.0, 8.0, 0.0, -1.0, -595.0, 0.0, 0.0, 258.0, 0.0, 0.0, 528.0, 0.0, 0.0, -279.0, 0.0, 0.0, -590.0, 0.0, 4.0, 252.0, 0.0, 2.0, 570.0, 0.0, -2.0, -244.0, 0.0, -1.0, -502.0, 0.0, 3.0, 250.0, 0.0, 2.0, -875.0, 0.0, 1.0, 29.0, 0.0, 0.0, -492.0, 0.0, -3.0, 275.0, 0.0, -1.0, 535.0, 0.0, -2.0, -228.0, 0.0, -1.0, -467.0, 0.0, 1.0, 240.0, 0.0, 1.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=111,120 ) /
            591.0, 0.0, 0.0, -253.0, 0.0, 0.0, -453.0, 0.0, -1.0, 244.0, 0.0, -1.0, 766.0, 0.0, 1.0, 9.0, 0.0, 0.0, -446.0, 0.0, 2.0, 225.0, 0.0, 1.0, -488.0, 0.0, 2.0, 207.0, 0.0, 1.0, -468.0, 0.0, 0.0, 201.0, 0.0, 0.0, -421.0, 0.0, 1.0, 216.0, 0.0, 1.0, 463.0, 0.0, 0.0, -200.0, 0.0, 0.0, -673.0, 0.0, 2.0, 14.0, 0.0, 0.0, 658.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=121,130 ) /
            -438.0, 0.0, 0.0, 188.0, 0.0, 0.0, -390.0, 0.0, 0.0, 205.0, 0.0, 0.0, 639.0, -11.0, -2.0, -19.0, 0.0, 0.0, 412.0, 0.0, -2.0, -176.0, 0.0, -1.0, -361.0, 0.0, 0.0, 189.0, 0.0, 0.0, 360.0, 0.0, -1.0, -185.0, 0.0, -1.0, 588.0, 0.0, -3.0, -24.0, 0.0, 0.0, -578.0, 0.0, 1.0, 5.0, 0.0, 0.0, -396.0, 0.0, 0.0, 171.0, 0.0, 0.0, 565.0, 0.0, -1.0, -6.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=131,140 ) /
            -335.0, 0.0, -1.0, 184.0, 0.0, -1.0, 357.0, 0.0, 1.0, -154.0, 0.0, 0.0, 321.0, 0.0, 1.0, -174.0, 0.0, 0.0, -301.0, 0.0, -1.0, 162.0, 0.0, 0.0, -334.0, 0.0, 0.0, 144.0, 0.0, 0.0, 493.0, 0.0, -2.0, -15.0, 0.0, 0.0, 494.0, 0.0, -2.0, -19.0, 0.0, 0.0, 337.0, 0.0, -1.0, -143.0, 0.0, -1.0, 280.0, 0.0, -1.0, -144.0, 0.0, 0.0, 309.0, 0.0, 1.0, -134.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=141,150 ) /
            -263.0, 0.0, 2.0, 131.0, 0.0, 1.0, 253.0, 0.0, 1.0, -138.0, 0.0, 0.0, 245.0, 0.0, 0.0, -128.0, 0.0, 0.0, 416.0, 0.0, -2.0, -17.0, 0.0, 0.0, -229.0, 0.0, 0.0, 128.0, 0.0, 0.0, 231.0, 0.0, 0.0, -120.0, 0.0, 0.0, -259.0, 0.0, 2.0, 109.0, 0.0, 1.0, 375.0, 0.0, -1.0, -8.0, 0.0, 0.0, 252.0, 0.0, 0.0, -108.0, 0.0, 0.0, -245.0, 0.0, 1.0, 104.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=151,160 ) /
            243.0, 0.0, -1.0, -104.0, 0.0, 0.0, 208.0, 0.0, 1.0, -112.0, 0.0, 0.0, 199.0, 0.0, 0.0, -102.0, 0.0, 0.0, -208.0, 0.0, 1.0, 105.0, 0.0, 0.0, 335.0, 0.0, -2.0, -14.0, 0.0, 0.0, -325.0, 0.0, 1.0, 7.0, 0.0, 0.0, -187.0, 0.0, 0.0, 96.0, 0.0, 0.0, 197.0, 0.0, -1.0, -100.0, 0.0, 0.0, -192.0, 0.0, 2.0, 94.0, 0.0, 1.0, -188.0, 0.0, 0.0, 83.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=161,170 ) /
            276.0, 0.0, 0.0, -2.0, 0.0, 0.0, -286.0, 0.0, 1.0, 6.0, 0.0, 0.0, 186.0, 0.0, -1.0, -79.0, 0.0, 0.0, -219.0, 0.0, 0.0, 43.0, 0.0, 0.0, 276.0, 0.0, 0.0, 2.0, 0.0, 0.0, -153.0, 0.0, -1.0, 84.0, 0.0, 0.0, -156.0, 0.0, 0.0, 81.0, 0.0, 0.0, -154.0, 0.0, 1.0, 78.0, 0.0, 0.0, -174.0, 0.0, 1.0, 75.0, 0.0, 0.0, -163.0, 0.0, 2.0, 69.0, 0.0, 1.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=171,180 ) /
            -228.0, 0.0, 0.0, 1.0, 0.0, 0.0, 91.0, 0.0, -4.0, -54.0, 0.0, -2.0, 175.0, 0.0, 0.0, -75.0, 0.0, 0.0, -159.0, 0.0, 0.0, 69.0, 0.0, 0.0, 141.0, 0.0, 0.0, -72.0, 0.0, 0.0, 147.0, 0.0, 0.0, -75.0, 0.0, 0.0, -132.0, 0.0, 0.0, 69.0, 0.0, 0.0, 159.0, 0.0, -28.0, -54.0, 0.0, 11.0, 213.0, 0.0, 0.0, -4.0, 0.0, 0.0, 123.0, 0.0, 0.0, -64.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=181,190 ) /
            -118.0, 0.0, -1.0, 66.0, 0.0, 0.0, 144.0, 0.0, -1.0, -61.0, 0.0, 0.0, -121.0, 0.0, 1.0, 60.0, 0.0, 0.0, -134.0, 0.0, 1.0, 56.0, 0.0, 1.0, -105.0, 0.0, 0.0, 57.0, 0.0, 0.0, -102.0, 0.0, 0.0, 56.0, 0.0, 0.0, 120.0, 0.0, 0.0, -52.0, 0.0, 0.0, 101.0, 0.0, 0.0, -54.0, 0.0, 0.0, -113.0, 0.0, 0.0, 59.0, 0.0, 0.0, -106.0, 0.0, 0.0, 61.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=191,200 ) /
            -129.0, 0.0, 1.0, 55.0, 0.0, 0.0, -114.0, 0.0, 0.0, 57.0, 0.0, 0.0, 113.0, 0.0, -1.0, -49.0, 0.0, 0.0, -102.0, 0.0, 0.0, 44.0, 0.0, 0.0, -94.0, 0.0, 0.0, 51.0, 0.0, 0.0, -100.0, 0.0, -1.0, 56.0, 0.0, 0.0, 87.0, 0.0, 0.0, -47.0, 0.0, 0.0, 161.0, 0.0, 0.0, -1.0, 0.0, 0.0, 96.0, 0.0, 0.0, -50.0, 0.0, 0.0, 151.0, 0.0, -1.0, -5.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=201,210 ) /
            -104.0, 0.0, 0.0, 44.0, 0.0, 0.0, -110.0, 0.0, 0.0, 48.0, 0.0, 0.0, -100.0, 0.0, 1.0, 50.0, 0.0, 0.0, 92.0, 0.0, -5.0, 12.0, 0.0, -2.0, 82.0, 0.0, 0.0, -45.0, 0.0, 0.0, 82.0, 0.0, 0.0, -45.0, 0.0, 0.0, -78.0, 0.0, 0.0, 41.0, 0.0, 0.0, -77.0, 0.0, 0.0, 43.0, 0.0, 0.0, 2.0, 0.0, 0.0, 54.0, 0.0, 0.0, 94.0, 0.0, 0.0, -40.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=211,220 ) /
            -93.0, 0.0, 0.0, 40.0, 0.0, 0.0, -83.0, 0.0, 10.0, 40.0, 0.0, -2.0, 83.0, 0.0, 0.0, -36.0, 0.0, 0.0, -91.0, 0.0, 0.0, 39.0, 0.0, 0.0, 128.0, 0.0, 0.0, -1.0, 0.0, 0.0, -79.0, 0.0, 0.0, 34.0, 0.0, 0.0, -83.0, 0.0, 0.0, 47.0, 0.0, 0.0, 84.0, 0.0, 0.0, -44.0, 0.0, 0.0, 83.0, 0.0, 0.0, -43.0, 0.0, 0.0, 91.0, 0.0, 0.0, -39.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=221,230 ) /
            -77.0, 0.0, 0.0, 39.0, 0.0, 0.0, 84.0, 0.0, 0.0, -43.0, 0.0, 0.0, -92.0, 0.0, 1.0, 39.0, 0.0, 0.0, -92.0, 0.0, 1.0, 39.0, 0.0, 0.0, -94.0, 0.0, 0.0, 0.0, 0.0, 0.0, 68.0, 0.0, 0.0, -36.0, 0.0, 0.0, -61.0, 0.0, 0.0, 32.0, 0.0, 0.0, 71.0, 0.0, 0.0, -31.0, 0.0, 0.0, 62.0, 0.0, 0.0, -34.0, 0.0, 0.0, -63.0, 0.0, 0.0, 33.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=231,240 ) /
            -73.0, 0.0, 0.0, 32.0, 0.0, 0.0, 115.0, 0.0, 0.0, -2.0, 0.0, 0.0, -103.0, 0.0, 0.0, 2.0, 0.0, 0.0, 63.0, 0.0, 0.0, -28.0, 0.0, 0.0, 74.0, 0.0, 0.0, -32.0, 0.0, 0.0, -103.0, 0.0, -3.0, 3.0, 0.0, -1.0, -69.0, 0.0, 0.0, 30.0, 0.0, 0.0, 57.0, 0.0, 0.0, -29.0, 0.0, 0.0, 94.0, 0.0, 0.0, -4.0, 0.0, 0.0, 64.0, 0.0, 0.0, -33.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=241,250 ) /
            -63.0, 0.0, 0.0, 26.0, 0.0, 0.0, -38.0, 0.0, 0.0, 20.0, 0.0, 0.0, -43.0, 0.0, 0.0, 24.0, 0.0, 0.0, -45.0, 0.0, 0.0, 23.0, 0.0, 0.0, 47.0, 0.0, 0.0, -24.0, 0.0, 0.0, -48.0, 0.0, 0.0, 25.0, 0.0, 0.0, 45.0, 0.0, 0.0, -26.0, 0.0, 0.0, 56.0, 0.0, 0.0, -25.0, 0.0, 0.0, 88.0, 0.0, 0.0, 2.0, 0.0, 0.0, -75.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=251,260 ) /
            85.0, 0.0, 0.0, 0.0, 0.0, 0.0, 49.0, 0.0, 0.0, -26.0, 0.0, 0.0, -74.0, 0.0, -3.0, -1.0, 0.0, -1.0, -39.0, 0.0, 0.0, 21.0, 0.0, 0.0, 45.0, 0.0, 0.0, -20.0, 0.0, 0.0, 51.0, 0.0, 0.0, -22.0, 0.0, 0.0, -40.0, 0.0, 0.0, 21.0, 0.0, 0.0, 41.0, 0.0, 0.0, -21.0, 0.0, 0.0, -42.0, 0.0, 0.0, 24.0, 0.0, 0.0, -51.0, 0.0, 0.0, 22.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=261,270 ) /
            -42.0, 0.0, 0.0, 22.0, 0.0, 0.0, 39.0, 0.0, 0.0, -21.0, 0.0, 0.0, 46.0, 0.0, 0.0, -18.0, 0.0, 0.0, -53.0, 0.0, 0.0, 22.0, 0.0, 0.0, 82.0, 0.0, 0.0, -4.0, 0.0, 0.0, 81.0, 0.0, -1.0, -4.0, 0.0, 0.0, 47.0, 0.0, 0.0, -19.0, 0.0, 0.0, 53.0, 0.0, 0.0, -23.0, 0.0, 0.0, -45.0, 0.0, 0.0, 22.0, 0.0, 0.0, -44.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=271,280 ) /
            -33.0, 0.0, 0.0, 16.0, 0.0, 0.0, -61.0, 0.0, 0.0, 1.0, 0.0, 0.0, 28.0, 0.0, 0.0, -15.0, 0.0, 0.0, -38.0, 0.0, 0.0, 19.0, 0.0, 0.0, -33.0, 0.0, 0.0, 21.0, 0.0, 0.0, -60.0, 0.0, 0.0, 0.0, 0.0, 0.0, 48.0, 0.0, 0.0, -10.0, 0.0, 0.0, 27.0, 0.0, 0.0, -14.0, 0.0, 0.0, 38.0, 0.0, 0.0, -20.0, 0.0, 0.0, 31.0, 0.0, 0.0, -13.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=281,290 ) /
            -29.0, 0.0, 0.0, 15.0, 0.0, 0.0, 28.0, 0.0, 0.0, -15.0, 0.0, 0.0, -32.0, 0.0, 0.0, 15.0, 0.0, 0.0, 45.0, 0.0, 0.0, -8.0, 0.0, 0.0, -44.0, 0.0, 0.0, 19.0, 0.0, 0.0, 28.0, 0.0, 0.0, -15.0, 0.0, 0.0, -51.0, 0.0, 0.0, 0.0, 0.0, 0.0, -36.0, 0.0, 0.0, 20.0, 0.0, 0.0, 44.0, 0.0, 0.0, -19.0, 0.0, 0.0, 26.0, 0.0, 0.0, -14.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=291,300 ) /
            -60.0, 0.0, 0.0, 2.0, 0.0, 0.0, 35.0, 0.0, 0.0, -18.0, 0.0, 0.0, -27.0, 0.0, 0.0, 11.0, 0.0, 0.0, 47.0, 0.0, 0.0, -1.0, 0.0, 0.0, 36.0, 0.0, 0.0, -15.0, 0.0, 0.0, -36.0, 0.0, 0.0, 20.0, 0.0, 0.0, -35.0, 0.0, 0.0, 19.0, 0.0, 0.0, -37.0, 0.0, 0.0, 19.0, 0.0, 0.0, 32.0, 0.0, 0.0, -16.0, 0.0, 0.0, 35.0, 0.0, 0.0, -14.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=301,310 ) /
            32.0, 0.0, 0.0, -13.0, 0.0, 0.0, 65.0, 0.0, 0.0, -2.0, 0.0, 0.0, 47.0, 0.0, 0.0, -1.0, 0.0, 0.0, 32.0, 0.0, 0.0, -16.0, 0.0, 0.0, 37.0, 0.0, 0.0, -16.0, 0.0, 0.0, -30.0, 0.0, 0.0, 15.0, 0.0, 0.0, -32.0, 0.0, 0.0, 16.0, 0.0, 0.0, -31.0, 0.0, 0.0, 13.0, 0.0, 0.0, 37.0, 0.0, 0.0, -16.0, 0.0, 0.0, 31.0, 0.0, 0.0, -13.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=311,320 ) /
            49.0, 0.0, 0.0, -2.0, 0.0, 0.0, 32.0, 0.0, 0.0, -13.0, 0.0, 0.0, 23.0, 0.0, 0.0, -12.0, 0.0, 0.0, -43.0, 0.0, 0.0, 18.0, 0.0, 0.0, 26.0, 0.0, 0.0, -11.0, 0.0, 0.0, -32.0, 0.0, 0.0, 14.0, 0.0, 0.0, -29.0, 0.0, 0.0, 14.0, 0.0, 0.0, -27.0, 0.0, 0.0, 12.0, 0.0, 0.0, 30.0, 0.0, 0.0, 0.0, 0.0, 0.0, -11.0, 0.0, 0.0, 5.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=321,330 ) /
            -21.0, 0.0, 0.0, 10.0, 0.0, 0.0, -34.0, 0.0, 0.0, 15.0, 0.0, 0.0, -10.0, 0.0, 0.0, 6.0, 0.0, 0.0, -36.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 0.0, 0.0, 4.0, 0.0, 0.0, -12.0, 0.0, 0.0, 5.0, 0.0, 0.0, -21.0, 0.0, 0.0, 5.0, 0.0, 0.0, -29.0, 0.0, 0.0, -1.0, 0.0, 0.0, -15.0, 0.0, 0.0, 3.0, 0.0, 0.0, -20.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=331,340 ) /
            28.0, 0.0, 0.0, 0.0, 0.0, -2.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, -22.0, 0.0, 0.0, 12.0, 0.0, 0.0, -14.0, 0.0, 0.0, 7.0, 0.0, 0.0, 24.0, 0.0, 0.0, -11.0, 0.0, 0.0, 11.0, 0.0, 0.0, -6.0, 0.0, 0.0, 14.0, 0.0, 0.0, -6.0, 0.0, 0.0, 24.0, 0.0, 0.0, 0.0, 0.0, 0.0, 18.0, 0.0, 0.0, -8.0, 0.0, 0.0, -38.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=341,350 ) /
            -31.0, 0.0, 0.0, 0.0, 0.0, 0.0, -16.0, 0.0, 0.0, 8.0, 0.0, 0.0, 29.0, 0.0, 0.0, 0.0, 0.0, 0.0, -18.0, 0.0, 0.0, 10.0, 0.0, 0.0, -10.0, 0.0, 0.0, 5.0, 0.0, 0.0, -17.0, 0.0, 0.0, 10.0, 0.0, 0.0, 9.0, 0.0, 0.0, -4.0, 0.0, 0.0, 16.0, 0.0, 0.0, -6.0, 0.0, 0.0, 22.0, 0.0, 0.0, -12.0, 0.0, 0.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=351,360 ) /
            -13.0, 0.0, 0.0, 6.0, 0.0, 0.0, -17.0, 0.0, 0.0, 9.0, 0.0, 0.0, -14.0, 0.0, 0.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 19.0, 0.0, 0.0, -10.0, 0.0, 0.0, -34.0, 0.0, 0.0, 0.0, 0.0, 0.0, -20.0, 0.0, 0.0, 8.0, 0.0, 0.0, 9.0, 0.0, 0.0, -5.0, 0.0, 0.0, -18.0, 0.0, 0.0, 7.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=361,370 ) /
            13.0, 0.0, 0.0, -6.0, 0.0, 0.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 5.0, 0.0, 0.0, 15.0, 0.0, 0.0, -8.0, 0.0, 0.0, -11.0, 0.0, 0.0, 3.0, 0.0, 0.0, 13.0, 0.0, 0.0, -5.0, 0.0, 0.0, -18.0, 0.0, 0.0, 0.0, 0.0, 0.0, -35.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, -4.0, 0.0, 0.0, -19.0, 0.0, 0.0, 10.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=371,380 ) /
            -26.0, 0.0, 0.0, 11.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0, 0.0, 0.0, 10.0, 0.0, 0.0, -6.0, 0.0, 0.0, -21.0, 0.0, 0.0, 9.0, 0.0, 0.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, -5.0, 0.0, 0.0, -29.0, 0.0, 0.0, 0.0, 0.0, 0.0, -19.0, 0.0, 0.0, 10.0, 0.0, 0.0, 12.0, 0.0, 0.0, -5.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=381,390 ) /
            22.0, 0.0, 0.0, -9.0, 0.0, 0.0, -10.0, 0.0, 0.0, 5.0, 0.0, 0.0, -20.0, 0.0, 0.0, 11.0, 0.0, 0.0, -20.0, 0.0, 0.0, 0.0, 0.0, 0.0, -17.0, 0.0, 0.0, 7.0, 0.0, 0.0, 15.0, 0.0, 0.0, -3.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, 0.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 6.0, 0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=391,400 ) /
            -13.0, 0.0, 0.0, 6.0, 0.0, 0.0, -14.0, 0.0, 0.0, 8.0, 0.0, 0.0, 13.0, 0.0, 0.0, -5.0, 0.0, 0.0, -17.0, 0.0, 0.0, 9.0, 0.0, 0.0, -12.0, 0.0, 0.0, 6.0, 0.0, 0.0, -10.0, 0.0, 0.0, 5.0, 0.0, 0.0, 10.0, 0.0, 0.0, -6.0, 0.0, 0.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, -22.0, 0.0, 0.0, 0.0, 0.0, 0.0, 28.0, 0.0, 0.0, -1.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=401,410 ) /
            15.0, 0.0, 0.0, -7.0, 0.0, 0.0, 23.0, 0.0, 0.0, -10.0, 0.0, 0.0, 12.0, 0.0, 0.0, -5.0, 0.0, 0.0, 29.0, 0.0, 0.0, -1.0, 0.0, 0.0, -25.0, 0.0, 0.0, 1.0, 0.0, 0.0, 22.0, 0.0, 0.0, 0.0, 0.0, 0.0, -18.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15.0, 0.0, 0.0, 3.0, 0.0, 0.0, -23.0, 0.0, 0.0, 0.0, 0.0, 0.0, 12.0, 0.0, 0.0, -5.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=411,420 ) /
            -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, -19.0, 0.0, 0.0, 0.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0, 0.0, 0.0, 21.0, 0.0, 0.0, -9.0, 0.0, 0.0, 23.0, 0.0, 0.0, -1.0, 0.0, 0.0, -16.0, 0.0, 0.0, 8.0, 0.0, 0.0, -19.0, 0.0, 0.0, 9.0, 0.0, 0.0, -22.0, 0.0, 0.0, 10.0, 0.0, 0.0, 27.0, 0.0, 0.0, -1.0, 0.0, 0.0, 16.0, 0.0, 0.0, -8.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=421,430 ) /
            19.0, 0.0, 0.0, -8.0, 0.0, 0.0, 9.0, 0.0, 0.0, -4.0, 0.0, 0.0, -9.0, 0.0, 0.0, 4.0, 0.0, 0.0, -9.0, 0.0, 0.0, 4.0, 0.0, 0.0, -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, 18.0, 0.0, 0.0, -9.0, 0.0, 0.0, 16.0, 0.0, 0.0, -1.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0, 0.0, 0.0, -23.0, 0.0, 0.0, 9.0, 0.0, 0.0, 16.0, 0.0, 0.0, -1.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=431,440 ) /
            -12.0, 0.0, 0.0, 6.0, 0.0, 0.0, -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, 30.0, 0.0, 0.0, -2.0, 0.0, 0.0, 24.0, 0.0, 0.0, -10.0, 0.0, 0.0, 10.0, 0.0, 0.0, -4.0, 0.0, 0.0, -16.0, 0.0, 0.0, 7.0, 0.0, 0.0, -16.0, 0.0, 0.0, 7.0, 0.0, 0.0, 17.0, 0.0, 0.0, -7.0, 0.0, 0.0, -24.0, 0.0, 0.0, 10.0, 0.0, 0.0, -12.0, 0.0, 0.0, 5.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=441,450 ) /
            -24.0, 0.0, 0.0, 11.0, 0.0, 0.0, -23.0, 0.0, 0.0, 9.0, 0.0, 0.0, -13.0, 0.0, 0.0, 5.0, 0.0, 0.0, -15.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, -1988.0, 0.0, 0.0, -1679.0, 0.0, 0.0, -63.0, 0.0, 0.0, -27.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 4.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 364.0, 0.0, 0.0, 176.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=451,460 ) /
            0.0, 0.0, -1044.0, 0.0, 0.0, -891.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 330.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=461,470 ) /
            3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=471,480 ) /
            -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, 7.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, -10.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=481,490 ) /
            -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 7.0, 0.0, 0.0, -3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 3.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=491,500 ) /
            -8.0, 0.0, 0.0, 3.0, 0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=501,510 ) /
            4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 9.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 9.0, 0.0, 0.0, -3.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=511,520 ) /
            -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=521,530 ) /
            3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, 3.0, 0.0, 0.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=531,540 ) /
            10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 10.0, 0.0, 13.0, 6.0, 0.0, -5.0, 0.0, 0.0, 30.0, 0.0, 0.0, 14.0, 0.0, 0.0, -162.0, 0.0, 0.0, -138.0, 0.0, 0.0, 75.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 4.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=541,550 ) /
            5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=551,560 ) /
            -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 0.0, -3.0, 3.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, -2.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=561,570 ) /
            -1.0, 0.0, 3.0, 3.0, 0.0, -1.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -13.0, 0.0, 0.0, -11.0, 3.0, 0.0, 6.0, 0.0, 0.0, 0.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=571,580 ) /
            8.0, 0.0, 0.0, -3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=581,590 ) /
            -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -8.0, 0.0, 0.0, 4.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -1.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=591,600 ) /
            -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, -26.0, 0.0, 0.0, -11.0, 0.0, 0.0, -10.0, 0.0, 0.0, -5.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=601,610 ) /
            3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 7.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -6.0, 0.0, 0.0, 2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, -7.0, 0.0, 0.0, 3.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=611,620 ) /
            13.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, -11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=621,630 ) /
            3.0, 0.0, 0.0, -2.0, 0.0, 0.0, -12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, -2.0, -7.0, 0.0, 0.0, 4.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=631,640 ) /
            6.0, 0.0, 0.0, -3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -5.0, 0.0, 0.0, 3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=641,650 ) /
            3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 6.0, 0.0, 0.0, -3.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=651,660 ) /
            -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 7.0, 0.0, 0.0, -4.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=661,670 ) /
            -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, 0.0, 0.0, -3.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0, -6.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, 0.0,
            // DATA ( ( CLS(I,J) / I=1,6 ) / J=671,678 ) /
            -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, 0.0)
}

internal object IAU2000_NAPL {
    /*
	 * Planetary argument multipliers L L' F D Om Me Ve E Ma Ju Sa Ur Ne pre
	 */
    var NAPL = doubleArrayOf(// DATA ( ( NAPL(I,J) / I=1,14 ) / J= 1, 10 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -16.0, 4.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 16.0, -4.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -16.0, 4.0, 5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 8.0, -1.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, 3.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 6.0, -3.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 11, 20 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -5.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -8.0, 1.0, 5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 6.0, 4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -5.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 2.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, -2.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 21, 30 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, 2.0, 2.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0, 3.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 19.0, -21.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 2.0, -4.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, -4.0, 10.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -7.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 31, 40 ) /
            -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 18.0, -16.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 1.0, 1.0, 2.0, 0.0, 0.0, 1.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 1.0, -1.0, 1.0, 0.0, 18.0, -17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 2.0, 0.0, -8.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -8.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 8.0, -14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 41, 50 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, -4.0, 5.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 2.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 3.0, -5.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -4.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 3.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 51, 60 ) /
            -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, -2.0, 0.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, -5.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 2.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 61, 70 ) /
            0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -9.0, 17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 17.0, -16.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 1.0, -3.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 9.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 71, 80 ) /
            0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 1.0, 0.0, 5.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 6.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 1.0, -3.0, 1.0, 0.0, -6.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 81, 90 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 15.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 15.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -9.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, -1.0, 0.0, 0.0, 0.0, 8.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -5.0, 5.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 0.0, 0.0, -6.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J= 91,100 ) /
            -2.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 6.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -1.0, -5.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 1.0, 1.0, 1.0, 0.0, -20.0, 20.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 20.0, -21.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 8.0, -15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -10.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=101,110 ) /
            0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 0.0, -6.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 2.0, 1.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=111,120 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -9.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 7.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, -17.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 17.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 1.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=121,130 ) /
            0.0, 0.0, -1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 2.0, 0.0, 1.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -5.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, -3.0, 1.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 8.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 8.0, -12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, -8.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 18.0, -16.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=131,140 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 3.0, -7.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, -3.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, -2.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -10.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 10.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=141,150 ) /
            0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 2.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, -5.0, 0.0, 0.0, 0.0, 2.0, 0.0, -1.0, -1.0, 1.0, 0.0, 0.0, 3.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -3.0, 7.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -18.0, 16.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, -8.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -8.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=151,160 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=161,170 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 3.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, -5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 5.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 5.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 6.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -8.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=171,180 ) /
            -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 6.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=181,190 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 13.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 0.0, 0.0, -5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -8.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=191,200 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=201,210 ) /
            0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 3.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=211,220 ) /
            0.0, 0.0, 2.0, -2.0, 2.0, 0.0, -3.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, 1.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=221,230 ) /
            0.0, 0.0, 2.0, -2.0, 2.0, 0.0, -5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -5.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 2.0, 0.0, 0.0, -1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=231,240 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 11.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 1.0, 0.0, -3.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -7.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=241,250 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 2.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 3.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, -3.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -4.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=251,260 ) /
            0.0, 0.0, 1.0, -1.0, 2.0, 0.0, -5.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 6.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -4.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 6.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 6.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 9.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 9.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=261,270 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=271,280 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 8.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 7.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=281,290 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 7.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 10.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=291,300 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 1.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, -4.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=301,310 ) /
            0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 4.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -4.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -4.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=311,320 ) /
            -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 5.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 12.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=321,330 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 4.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 10.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=331,340 ) /
            0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -3.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 7.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 8.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 3.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=341,350 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 6.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=351,360 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=361,370 ) /
            0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -3.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=371,380 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -8.0, 3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=381,390 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 8.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -2.0, 5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, -5.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=391,400 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=401,410 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 6.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 9.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=411,420 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 7.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=421,430 ) /
            0.0, 0.0, 1.0, -1.0, 1.0, 0.0, -1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=431,440 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 5.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 13.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 5.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 4.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 7.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=441,450 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 9.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=451,460 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=461,470 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 13.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 15.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 15.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=471,480 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 9.0, -4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 8.0, -1.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -8.0, 3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=481,490 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 16.0, -4.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 8.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 8.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -8.0, 1.0, 5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -5.0, 4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=491,500 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -3.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=501,510 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 6.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -9.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -7.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -5.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=511,520 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -3.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -1.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -4.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=521,530 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 5.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 12.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -8.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=531,540 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -6.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=541,550 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, -1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 1.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 16.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 2.0, -5.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -8.0, 3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 16.0, -4.0, -5.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=551,560 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 8.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -3.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -5.0, 5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=561,570 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -7.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -7.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -5.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -3.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=571,580 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 11.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, -4.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -6.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, -2.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=581,590 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -4.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, -1.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=591,600 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -7.0, 7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 7.0, -7.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, -4.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=601,610 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, -3.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 0.0, -2.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -8.0, 8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 8.0, -8.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -9.0, 9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=611,620 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 9.0, -9.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, -4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=621,630 ) /
            0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=631,640 ) /
            -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 1.0, -1.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=641,650 ) /
            -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 2.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -2.0, 1.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, -2.0, 2.0, 0.0, -3.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, -2.0, 2.0, 0.0, 0.0, -2.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=651,660 ) /
            0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, -2.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, -2.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 10.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=661,670 ) /
            0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, -2.0, 2.0, 0.0, 0.0, -2.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, -2.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -2.0, 0.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=671,680 ) /
            0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, -2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 2.0, 2.0, 0.0, 0.0, -1.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, -1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 2.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, -4.0, 8.0, -3.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 4.0, -8.0, 3.0, 0.0, 0.0, 0.0, 0.0,
            // DATA ( ( NAPL(I,J) / I=1,14 ) / J=681,687 ) /
            1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 2.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0, 2.0, 2.0, 0.0, 3.0, -3.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 2.0, 0.0, 2.0, 0.0, 1.0, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0, 2.0, 2.0, 0.0, 0.0, 2.0, 0.0, -2.0, 0.0, 0.0, 0.0, 0.0)
}

internal object IAU2000_ICPL {
    /*
	 * Planetary nutation coefficients, unit 1e-7 arcsec longitude (sin, cos) /
	 * obliquity (sin, cos)
	 */
    var ICPL = doubleArrayOf(// DATA ( ( ICPL(I,J) / I=1,4 ) / J= 1, 10 ) /
            1440.0, 0.0, 0.0, 0.0, 56.0, -117.0, -42.0, -40.0, 125.0, -43.0, 0.0, -54.0, 0.0, 5.0, 0.0, 0.0, 3.0, -7.0, -3.0, 0.0, 3.0, 0.0, 0.0, -2.0, -114.0, 0.0, 0.0, 61.0, -219.0, 89.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, -462.0, 1604.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 11, 20 ) /
            99.0, 0.0, 0.0, -53.0, -3.0, 0.0, 0.0, 2.0, 0.0, 6.0, 2.0, 0.0, 3.0, 0.0, 0.0, 0.0, -12.0, 0.0, 0.0, 0.0, 14.0, -218.0, 117.0, 8.0, 31.0, -481.0, -257.0, -17.0, -491.0, 128.0, 0.0, 0.0, -3084.0, 5123.0, 2735.0, 1647.0, -1444.0, 2409.0, -1286.0, -771.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 21, 30 ) /
            11.0, -24.0, -11.0, -9.0, 26.0, -9.0, 0.0, 0.0, 103.0, -60.0, 0.0, 0.0, 0.0, -13.0, -7.0, 0.0, -26.0, -29.0, -16.0, 14.0, 9.0, -27.0, -14.0, -5.0, 12.0, 0.0, 0.0, -6.0, -7.0, 0.0, 0.0, 0.0, 0.0, 24.0, 0.0, 0.0, 284.0, 0.0, 0.0, -151.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 31, 40 ) /
            226.0, 101.0, 0.0, 0.0, 0.0, -8.0, -2.0, 0.0, 0.0, -6.0, -3.0, 0.0, 5.0, 0.0, 0.0, -3.0, -41.0, 175.0, 76.0, 17.0, 0.0, 15.0, 6.0, 0.0, 425.0, 212.0, -133.0, 269.0, 1200.0, 598.0, 319.0, -641.0, 235.0, 334.0, 0.0, 0.0, 11.0, -12.0, -7.0, -6.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 41, 50 ) /
            5.0, -6.0, 3.0, 3.0, -5.0, 0.0, 0.0, 3.0, 6.0, 0.0, 0.0, -3.0, 15.0, 0.0, 0.0, 0.0, 13.0, 0.0, 0.0, -7.0, -6.0, -9.0, 0.0, 0.0, 266.0, -78.0, 0.0, 0.0, -460.0, -435.0, -232.0, 246.0, 0.0, 15.0, 7.0, 0.0, -3.0, 0.0, 0.0, 2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 51, 60 ) /
            0.0, 131.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 4.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -17.0, -19.0, -10.0, 9.0, -9.0, -11.0, 6.0, -5.0, -6.0, 0.0, 0.0, 3.0, -16.0, 8.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 61, 70 ) /
            11.0, 24.0, 11.0, -5.0, -3.0, -4.0, -2.0, 1.0, 3.0, 0.0, 0.0, -1.0, 0.0, -8.0, -4.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0, 3.0, 2.0, 0.0, -6.0, 4.0, 2.0, 3.0, -3.0, -5.0, 0.0, 0.0, -5.0, 0.0, 0.0, 2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 71, 80 ) /
            4.0, 24.0, 13.0, -2.0, -42.0, 20.0, 0.0, 0.0, -10.0, 233.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 78.0, -18.0, 0.0, 0.0, 0.0, 3.0, 1.0, 0.0, 0.0, -3.0, -1.0, 0.0, 0.0, -4.0, -2.0, 1.0, 0.0, -8.0, -4.0, -1.0, 0.0, -5.0, 3.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 81, 90 ) /
            -7.0, 0.0, 0.0, 3.0, -14.0, 8.0, 3.0, 6.0, 0.0, 8.0, -4.0, 0.0, 0.0, 19.0, 10.0, 0.0, 45.0, -22.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 3.0, 5.0, 3.0, -2.0, 89.0, -16.0, -9.0, -48.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J= 91,100 ) /
            0.0, 3.0, 0.0, 0.0, -3.0, 7.0, 4.0, 2.0, -349.0, -62.0, 0.0, 0.0, -15.0, 22.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, -53.0, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, -8.0, 0.0, 0.0, 15.0, -7.0, -4.0, -8.0, -3.0, 0.0, 0.0, 1.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=101,110 ) /
            -21.0, -78.0, 0.0, 0.0, 20.0, -70.0, -37.0, -11.0, 0.0, 6.0, 3.0, 0.0, 5.0, 3.0, 2.0, -2.0, -17.0, -4.0, -2.0, 9.0, 0.0, 6.0, 3.0, 0.0, 32.0, 15.0, -8.0, 17.0, 174.0, 84.0, 45.0, -93.0, 11.0, 56.0, 0.0, 0.0, -66.0, -12.0, -6.0, 35.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=111,120 ) /
            47.0, 8.0, 4.0, -25.0, 0.0, 8.0, 4.0, 0.0, 10.0, -22.0, -12.0, -5.0, -3.0, 0.0, 0.0, 2.0, -24.0, 12.0, 0.0, 0.0, 5.0, -6.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 4.0, 3.0, 1.0, -2.0, 0.0, 29.0, 15.0, 0.0, -5.0, -4.0, -2.0, 2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=121,130 ) /
            8.0, -3.0, -1.0, -5.0, 0.0, -3.0, 0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, -5.0, 0.0, 0.0, 3.0, 46.0, 66.0, 35.0, -25.0, -14.0, 7.0, 0.0, 0.0, 0.0, 3.0, 2.0, 0.0, -5.0, 0.0, 0.0, 0.0, -68.0, -34.0, -18.0, 36.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=131,140 ) /
            0.0, 14.0, 7.0, 0.0, 10.0, -6.0, -3.0, -5.0, -5.0, -4.0, -2.0, 3.0, -3.0, 5.0, 2.0, 1.0, 76.0, 17.0, 9.0, -41.0, 84.0, 298.0, 159.0, -45.0, 3.0, 0.0, 0.0, -1.0, -3.0, 0.0, 0.0, 2.0, -3.0, 0.0, 0.0, 1.0, -82.0, 292.0, 156.0, 44.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=141,150 ) /
            -73.0, 17.0, 9.0, 39.0, -9.0, -16.0, 0.0, 0.0, 3.0, 0.0, -1.0, -2.0, -3.0, 0.0, 0.0, 0.0, -9.0, -5.0, -3.0, 5.0, -439.0, 0.0, 0.0, 0.0, 57.0, -28.0, -15.0, -30.0, 0.0, -6.0, -3.0, 0.0, -4.0, 0.0, 0.0, 2.0, -40.0, 57.0, 30.0, 21.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=151,160 ) /
            23.0, 7.0, 3.0, -13.0, 273.0, 80.0, 43.0, -146.0, -449.0, 430.0, 0.0, 0.0, -8.0, -47.0, -25.0, 4.0, 6.0, 47.0, 25.0, -3.0, 0.0, 23.0, 13.0, 0.0, -3.0, 0.0, 0.0, 2.0, 3.0, -4.0, -2.0, -2.0, -48.0, -110.0, -59.0, 26.0, 51.0, 114.0, 61.0, -27.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=161,170 ) /
            -133.0, 0.0, 0.0, 57.0, 0.0, 4.0, 0.0, 0.0, -21.0, -6.0, -3.0, 11.0, 0.0, -3.0, -1.0, 0.0, -11.0, -21.0, -11.0, 6.0, -18.0, -436.0, -233.0, 9.0, 35.0, -7.0, 0.0, 0.0, 0.0, 5.0, 3.0, 0.0, 11.0, -3.0, -1.0, -6.0, -5.0, -3.0, -1.0, 3.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=171,180 ) /
            -53.0, -9.0, -5.0, 28.0, 0.0, 3.0, 2.0, 1.0, 4.0, 0.0, 0.0, -2.0, 0.0, -4.0, 0.0, 0.0, -50.0, 194.0, 103.0, 27.0, -13.0, 52.0, 28.0, 7.0, -91.0, 248.0, 0.0, 0.0, 6.0, 49.0, 26.0, -3.0, -6.0, -47.0, -25.0, 3.0, 0.0, 5.0, 3.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=181,190 ) /
            52.0, 23.0, 10.0, -23.0, -3.0, 0.0, 0.0, 1.0, 0.0, 5.0, 3.0, 0.0, -4.0, 0.0, 0.0, 0.0, -4.0, 8.0, 3.0, 2.0, 10.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 0.0, 8.0, 4.0, 0.0, 0.0, 8.0, 4.0, 1.0, -4.0, 0.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=191,200 ) /
            -4.0, 0.0, 0.0, 0.0, -8.0, 4.0, 2.0, 4.0, 8.0, -4.0, -2.0, -4.0, 0.0, 15.0, 7.0, 0.0, -138.0, 0.0, 0.0, 0.0, 0.0, -7.0, -3.0, 0.0, 0.0, -7.0, -3.0, 0.0, 54.0, 0.0, 0.0, -29.0, 0.0, 10.0, 4.0, 0.0, -7.0, 0.0, 0.0, 3.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=201,210 ) /
            -37.0, 35.0, 19.0, 20.0, 0.0, 4.0, 0.0, 0.0, -4.0, 9.0, 0.0, 0.0, 8.0, 0.0, 0.0, -4.0, -9.0, -14.0, -8.0, 5.0, -3.0, -9.0, -5.0, 3.0, -145.0, 47.0, 0.0, 0.0, -10.0, 40.0, 21.0, 5.0, 11.0, -49.0, -26.0, -7.0, -2150.0, 0.0, 0.0, 932.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=211,220 ) /
            -12.0, 0.0, 0.0, 5.0, 85.0, 0.0, 0.0, -37.0, 4.0, 0.0, 0.0, -2.0, 3.0, 0.0, 0.0, -2.0, -86.0, 153.0, 0.0, 0.0, -6.0, 9.0, 5.0, 3.0, 9.0, -13.0, -7.0, -5.0, -8.0, 12.0, 6.0, 4.0, -51.0, 0.0, 0.0, 22.0, -11.0, -268.0, -116.0, 5.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=221,230 ) /
            0.0, 12.0, 5.0, 0.0, 0.0, 7.0, 3.0, 0.0, 31.0, 6.0, 3.0, -17.0, 140.0, 27.0, 14.0, -75.0, 57.0, 11.0, 6.0, -30.0, -14.0, -39.0, 0.0, 0.0, 0.0, -6.0, -2.0, 0.0, 4.0, 15.0, 8.0, -2.0, 0.0, 4.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=231,240 ) /
            0.0, 11.0, 5.0, 0.0, 9.0, 6.0, 0.0, 0.0, -4.0, 10.0, 4.0, 2.0, 5.0, 3.0, 0.0, 0.0, 16.0, 0.0, 0.0, -9.0, -3.0, 0.0, 0.0, 0.0, 0.0, 3.0, 2.0, -1.0, 7.0, 0.0, 0.0, -3.0, -25.0, 22.0, 0.0, 0.0, 42.0, 223.0, 119.0, -22.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=241,250 ) /
            -27.0, -143.0, -77.0, 14.0, 9.0, 49.0, 26.0, -5.0, -1166.0, 0.0, 0.0, 505.0, -5.0, 0.0, 0.0, 2.0, -6.0, 0.0, 0.0, 3.0, -8.0, 0.0, 1.0, 4.0, 0.0, -4.0, 0.0, 0.0, 117.0, 0.0, 0.0, -63.0, -4.0, 8.0, 4.0, 2.0, 3.0, 0.0, 0.0, -2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=251,260 ) /
            -5.0, 0.0, 0.0, 2.0, 0.0, 31.0, 0.0, 0.0, -5.0, 0.0, 1.0, 3.0, 4.0, 0.0, 0.0, -2.0, -4.0, 0.0, 0.0, 2.0, -24.0, -13.0, -6.0, 10.0, 3.0, 0.0, 0.0, 0.0, 0.0, -32.0, -17.0, 0.0, 8.0, 12.0, 5.0, -3.0, 3.0, 0.0, 0.0, -1.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=261,270 ) /
            7.0, 13.0, 0.0, 0.0, -3.0, 16.0, 0.0, 0.0, 50.0, 0.0, 0.0, -27.0, 0.0, -5.0, -3.0, 0.0, 13.0, 0.0, 0.0, 0.0, 0.0, 5.0, 3.0, 1.0, 24.0, 5.0, 2.0, -11.0, 5.0, -11.0, -5.0, -2.0, 30.0, -3.0, -2.0, -16.0, 18.0, 0.0, 0.0, -9.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=271,280 ) /
            8.0, 614.0, 0.0, 0.0, 3.0, -3.0, -1.0, -2.0, 6.0, 17.0, 9.0, -3.0, -3.0, -9.0, -5.0, 2.0, 0.0, 6.0, 3.0, -1.0, -127.0, 21.0, 9.0, 55.0, 3.0, 5.0, 0.0, 0.0, -6.0, -10.0, -4.0, 3.0, 5.0, 0.0, 0.0, 0.0, 16.0, 9.0, 4.0, -7.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=281,290 ) /
            3.0, 0.0, 0.0, -2.0, 0.0, 22.0, 0.0, 0.0, 0.0, 19.0, 10.0, 0.0, 7.0, 0.0, 0.0, -4.0, 0.0, -5.0, -2.0, 0.0, 0.0, 3.0, 1.0, 0.0, -9.0, 3.0, 1.0, 4.0, 17.0, 0.0, 0.0, -7.0, 0.0, -3.0, -2.0, -1.0, -20.0, 34.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=291,300 ) /
            -10.0, 0.0, 1.0, 5.0, -4.0, 0.0, 0.0, 2.0, 22.0, -87.0, 0.0, 0.0, -4.0, 0.0, 0.0, 2.0, -3.0, -6.0, -2.0, 1.0, -16.0, -3.0, -1.0, 7.0, 0.0, -3.0, -2.0, 0.0, 4.0, 0.0, 0.0, 0.0, -68.0, 39.0, 0.0, 0.0, 27.0, 0.0, 0.0, -14.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=301,310 ) /
            0.0, -4.0, 0.0, 0.0, -25.0, 0.0, 0.0, 0.0, -12.0, -3.0, -2.0, 6.0, 3.0, 0.0, 0.0, -1.0, 3.0, 66.0, 29.0, -1.0, 490.0, 0.0, 0.0, -213.0, -22.0, 93.0, 49.0, 12.0, -7.0, 28.0, 15.0, 4.0, -3.0, 13.0, 7.0, 2.0, -46.0, 14.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=311,320 ) /
            -5.0, 0.0, 0.0, 0.0, 2.0, 1.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, -28.0, 0.0, 0.0, 15.0, 5.0, 0.0, 0.0, -2.0, 0.0, 3.0, 0.0, 0.0, -11.0, 0.0, 0.0, 5.0, 0.0, 3.0, 1.0, 0.0, -3.0, 0.0, 0.0, 1.0, 25.0, 106.0, 57.0, -13.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=321,330 ) /
            5.0, 21.0, 11.0, -3.0, 1485.0, 0.0, 0.0, 0.0, -7.0, -32.0, -17.0, 4.0, 0.0, 5.0, 3.0, 0.0, -6.0, -3.0, -2.0, 3.0, 30.0, -6.0, -2.0, -13.0, -4.0, 4.0, 0.0, 0.0, -19.0, 0.0, 0.0, 10.0, 0.0, 4.0, 2.0, -1.0, 0.0, 3.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=331,340 ) /
            4.0, 0.0, 0.0, -2.0, 0.0, -3.0, -1.0, 0.0, -3.0, 0.0, 0.0, 0.0, 5.0, 3.0, 1.0, -2.0, 0.0, 11.0, 0.0, 0.0, 118.0, 0.0, 0.0, -52.0, 0.0, -5.0, -3.0, 0.0, -28.0, 36.0, 0.0, 0.0, 5.0, -5.0, 0.0, 0.0, 14.0, -59.0, -31.0, -8.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=341,350 ) /
            0.0, 9.0, 5.0, 1.0, -458.0, 0.0, 0.0, 198.0, 0.0, -45.0, -20.0, 0.0, 9.0, 0.0, 0.0, -5.0, 0.0, -3.0, 0.0, 0.0, 0.0, -4.0, -2.0, -1.0, 11.0, 0.0, 0.0, -6.0, 6.0, 0.0, 0.0, -2.0, -16.0, 23.0, 0.0, 0.0, 0.0, -4.0, -2.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=351,360 ) /
            -5.0, 0.0, 0.0, 2.0, -166.0, 269.0, 0.0, 0.0, 15.0, 0.0, 0.0, -8.0, 10.0, 0.0, 0.0, -4.0, -78.0, 45.0, 0.0, 0.0, 0.0, -5.0, -2.0, 0.0, 7.0, 0.0, 0.0, -4.0, -5.0, 328.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, 5.0, 0.0, 0.0, -2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=361,370 ) /
            0.0, 3.0, 1.0, 0.0, -3.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -4.0, -2.0, 0.0, -1223.0, -26.0, 0.0, 0.0, 0.0, 7.0, 3.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, 3.0, 2.0, 0.0, -6.0, 20.0, 0.0, 0.0, -368.0, 0.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=371,380 ) /
            -75.0, 0.0, 0.0, 0.0, 11.0, 0.0, 0.0, -6.0, 3.0, 0.0, 0.0, -2.0, -3.0, 0.0, 0.0, 1.0, -13.0, -30.0, 0.0, 0.0, 21.0, 3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, -4.0, 0.0, 0.0, 2.0, 8.0, -27.0, 0.0, 0.0, -19.0, -11.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=381,390 ) /
            -4.0, 0.0, 0.0, 2.0, 0.0, 5.0, 2.0, 0.0, -6.0, 0.0, 0.0, 2.0, -8.0, 0.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, -14.0, 0.0, 0.0, 6.0, 6.0, 0.0, 0.0, 0.0, -74.0, 0.0, 0.0, 32.0, 0.0, -3.0, -1.0, 0.0, 4.0, 0.0, 0.0, -2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=391,400 ) /
            8.0, 11.0, 0.0, 0.0, 0.0, 3.0, 2.0, 0.0, -262.0, 0.0, 0.0, 114.0, 0.0, -4.0, 0.0, 0.0, -7.0, 0.0, 0.0, 4.0, 0.0, -27.0, -12.0, 0.0, -19.0, -8.0, -4.0, 8.0, 202.0, 0.0, 0.0, -87.0, -8.0, 35.0, 19.0, 5.0, 0.0, 4.0, 2.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=401,410 ) /
            16.0, -5.0, 0.0, 0.0, 5.0, 0.0, 0.0, -3.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, -35.0, -48.0, -21.0, 15.0, -3.0, -5.0, -2.0, 1.0, 6.0, 0.0, 0.0, -3.0, 3.0, 0.0, 0.0, -1.0, 0.0, -5.0, 0.0, 0.0, 12.0, 55.0, 29.0, -6.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=411,420 ) /
            0.0, 5.0, 3.0, 0.0, -598.0, 0.0, 0.0, 0.0, -3.0, -13.0, -7.0, 1.0, -5.0, -7.0, -3.0, 2.0, 3.0, 0.0, 0.0, -1.0, 5.0, -7.0, 0.0, 0.0, 4.0, 0.0, 0.0, -2.0, 16.0, -6.0, 0.0, 0.0, 8.0, -3.0, 0.0, 0.0, 8.0, -31.0, -16.0, -4.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=421,430 ) /
            0.0, 3.0, 1.0, 0.0, 113.0, 0.0, 0.0, -49.0, 0.0, -24.0, -10.0, 0.0, 4.0, 0.0, 0.0, -2.0, 27.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, -4.0, -2.0, 0.0, 5.0, 0.0, 0.0, -2.0, 0.0, -3.0, 0.0, 0.0, -13.0, 0.0, 0.0, 6.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=431,440 ) /
            5.0, 0.0, 0.0, -2.0, -18.0, -10.0, -4.0, 8.0, -4.0, -28.0, 0.0, 0.0, -5.0, 6.0, 3.0, 2.0, -3.0, 0.0, 0.0, 1.0, -5.0, -9.0, -4.0, 2.0, 17.0, 0.0, 0.0, -7.0, 11.0, 4.0, 0.0, 0.0, 0.0, -6.0, -2.0, 0.0, 83.0, 15.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=441,450 ) /
            -4.0, 0.0, 0.0, 2.0, 0.0, -114.0, -49.0, 0.0, 117.0, 0.0, 0.0, -51.0, -5.0, 19.0, 10.0, 2.0, -3.0, 0.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 0.0, -3.0, -1.0, 0.0, 3.0, 0.0, 0.0, 0.0, 0.0, -6.0, -2.0, 0.0, 393.0, 3.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=451,460 ) /
            -4.0, 21.0, 11.0, 2.0, -6.0, 0.0, -1.0, 3.0, -3.0, 8.0, 4.0, 1.0, 8.0, 0.0, 0.0, 0.0, 18.0, -29.0, -13.0, -8.0, 8.0, 34.0, 18.0, -4.0, 89.0, 0.0, 0.0, 0.0, 3.0, 12.0, 6.0, -1.0, 54.0, -15.0, -7.0, -24.0, 0.0, 3.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=461,470 ) /
            3.0, 0.0, 0.0, -1.0, 0.0, 35.0, 0.0, 0.0, -154.0, -30.0, -13.0, 67.0, 15.0, 0.0, 0.0, 0.0, 0.0, 4.0, 2.0, 0.0, 0.0, 9.0, 0.0, 0.0, 80.0, -71.0, -31.0, -35.0, 0.0, -20.0, -9.0, 0.0, 11.0, 5.0, 2.0, -5.0, 61.0, -96.0, -42.0, -27.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=471,480 ) /
            14.0, 9.0, 4.0, -6.0, -11.0, -6.0, -3.0, 5.0, 0.0, -3.0, -1.0, 0.0, 123.0, -415.0, -180.0, -53.0, 0.0, 0.0, 0.0, -35.0, -5.0, 0.0, 0.0, 0.0, 7.0, -32.0, -17.0, -4.0, 0.0, -9.0, -5.0, 0.0, 0.0, -4.0, 2.0, 0.0, -89.0, 0.0, 0.0, 38.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=481,490 ) /
            0.0, -86.0, -19.0, -6.0, 0.0, 0.0, -19.0, 6.0, -123.0, -416.0, -180.0, 53.0, 0.0, -3.0, -1.0, 0.0, 12.0, -6.0, -3.0, -5.0, -13.0, 9.0, 4.0, 6.0, 0.0, -15.0, -7.0, 0.0, 3.0, 0.0, 0.0, -1.0, -62.0, -97.0, -42.0, 27.0, -11.0, 5.0, 2.0, 5.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=491,500 ) /
            0.0, -19.0, -8.0, 0.0, -3.0, 0.0, 0.0, 1.0, 0.0, 4.0, 2.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0, 4.0, 2.0, 0.0, -85.0, -70.0, -31.0, 37.0, 163.0, -12.0, -5.0, -72.0, -63.0, -16.0, -7.0, 28.0, -21.0, -32.0, -14.0, 9.0, 0.0, -3.0, -1.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=501,510 ) /
            3.0, 0.0, 0.0, -2.0, 0.0, 8.0, 0.0, 0.0, 3.0, 10.0, 4.0, -1.0, 3.0, 0.0, 0.0, -1.0, 0.0, -7.0, -3.0, 0.0, 0.0, -4.0, -2.0, 0.0, 6.0, 19.0, 0.0, 0.0, 5.0, -173.0, -75.0, -2.0, 0.0, -7.0, -3.0, 0.0, 7.0, -12.0, -5.0, -3.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=511,520 ) /
            -3.0, 0.0, 0.0, 2.0, 3.0, -4.0, -2.0, -1.0, 74.0, 0.0, 0.0, -32.0, -3.0, 12.0, 6.0, 2.0, 26.0, -14.0, -6.0, -11.0, 19.0, 0.0, 0.0, -8.0, 6.0, 24.0, 13.0, -3.0, 83.0, 0.0, 0.0, 0.0, 0.0, -10.0, -5.0, 0.0, 11.0, -3.0, -1.0, -5.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=521,530 ) /
            3.0, 0.0, 1.0, -1.0, 3.0, 0.0, 0.0, -1.0, -4.0, 0.0, 0.0, 0.0, 5.0, -23.0, -12.0, -3.0, -339.0, 0.0, 0.0, 147.0, 0.0, -10.0, -5.0, 0.0, 5.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 0.0, -4.0, -2.0, 0.0, 18.0, -3.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=531,540 ) /
            9.0, -11.0, -5.0, -4.0, -8.0, 0.0, 0.0, 4.0, 3.0, 0.0, 0.0, -1.0, 0.0, 9.0, 0.0, 0.0, 6.0, -9.0, -4.0, -2.0, -4.0, -12.0, 0.0, 0.0, 67.0, -91.0, -39.0, -29.0, 30.0, -18.0, -8.0, -13.0, 0.0, 0.0, 0.0, 0.0, 0.0, -114.0, -50.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=541,550 ) /
            0.0, 0.0, 0.0, 23.0, 517.0, 16.0, 7.0, -224.0, 0.0, -7.0, -3.0, 0.0, 143.0, -3.0, -1.0, -62.0, 29.0, 0.0, 0.0, -13.0, -4.0, 0.0, 0.0, 2.0, -6.0, 0.0, 0.0, 3.0, 5.0, 12.0, 5.0, -2.0, -25.0, 0.0, 0.0, 11.0, -3.0, 0.0, 0.0, 1.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=551,560 ) /
            0.0, 4.0, 2.0, 0.0, -22.0, 12.0, 5.0, 10.0, 50.0, 0.0, 0.0, -22.0, 0.0, 7.0, 4.0, 0.0, 0.0, 3.0, 1.0, 0.0, -4.0, 4.0, 2.0, 2.0, -5.0, -11.0, -5.0, 2.0, 0.0, 4.0, 2.0, 0.0, 4.0, 17.0, 9.0, -2.0, 59.0, 0.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=561,570 ) /
            0.0, -4.0, -2.0, 0.0, -8.0, 0.0, 0.0, 4.0, -3.0, 0.0, 0.0, 0.0, 4.0, -15.0, -8.0, -2.0, 370.0, -8.0, 0.0, -160.0, 0.0, 0.0, -3.0, 0.0, 0.0, 3.0, 1.0, 0.0, -6.0, 3.0, 1.0, 3.0, 0.0, 6.0, 0.0, 0.0, -10.0, 0.0, 0.0, 4.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=571,580 ) /
            0.0, 9.0, 4.0, 0.0, 4.0, 17.0, 7.0, -2.0, 34.0, 0.0, 0.0, -15.0, 0.0, 5.0, 3.0, 0.0, -5.0, 0.0, 0.0, 2.0, -37.0, -7.0, -3.0, 16.0, 3.0, 13.0, 7.0, -2.0, 40.0, 0.0, 0.0, 0.0, 0.0, -3.0, -2.0, 0.0, -184.0, -3.0, -1.0, 80.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=581,590 ) /
            -3.0, 0.0, 0.0, 1.0, -3.0, 0.0, 0.0, 0.0, 0.0, -10.0, -6.0, -1.0, 31.0, -6.0, 0.0, -13.0, -3.0, -32.0, -14.0, 1.0, -7.0, 0.0, 0.0, 3.0, 0.0, -8.0, -4.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 3.0, 1.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=591,600 ) /
            19.0, -23.0, -10.0, 2.0, 0.0, 0.0, 0.0, -10.0, 0.0, 3.0, 2.0, 0.0, 0.0, 9.0, 5.0, -1.0, 28.0, 0.0, 0.0, 0.0, 0.0, -7.0, -4.0, 0.0, 8.0, -4.0, 0.0, -4.0, 0.0, 0.0, -2.0, 0.0, 0.0, 3.0, 0.0, 0.0, -3.0, 0.0, 0.0, 1.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=601,610 ) /
            -9.0, 0.0, 1.0, 4.0, 3.0, 12.0, 5.0, -1.0, 17.0, -3.0, -1.0, 0.0, 0.0, 7.0, 4.0, 0.0, 19.0, 0.0, 0.0, 0.0, 0.0, -5.0, -3.0, 0.0, 14.0, -3.0, 0.0, -1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 0.0, -5.0, 0.0, 5.0, 3.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=611,620 ) /
            13.0, 0.0, 0.0, 0.0, 0.0, -3.0, -2.0, 0.0, 2.0, 9.0, 4.0, 3.0, 0.0, 0.0, 0.0, -4.0, 8.0, 0.0, 0.0, 0.0, 0.0, 4.0, 2.0, 0.0, 6.0, 0.0, 0.0, -3.0, 6.0, 0.0, 0.0, 0.0, 0.0, 3.0, 1.0, 0.0, 5.0, 0.0, 0.0, -2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=621,630 ) /
            3.0, 0.0, 0.0, -1.0, -3.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 7.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 0.0, -4.0, 0.0, 0.0, 5.0, 0.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=631,640 ) /
            -3.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 13.0, 0.0, 0.0, 0.0, 21.0, 11.0, 0.0, 0.0, 0.0, -5.0, 0.0, 0.0, 0.0, -5.0, -2.0, 0.0, 0.0, 5.0, 3.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=641,650 ) /
            0.0, -5.0, 0.0, 0.0, -3.0, 0.0, 0.0, 2.0, 20.0, 10.0, 0.0, 0.0, -34.0, 0.0, 0.0, 0.0, -19.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -2.0, -3.0, 0.0, 0.0, 1.0, -6.0, 0.0, 0.0, 3.0, -4.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=651,660 ) /
            3.0, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0, 0.0, 3.0, 0.0, 0.0, -1.0, 6.0, 0.0, 0.0, -3.0, -8.0, 0.0, 0.0, 3.0, 0.0, 3.0, 1.0, 0.0, -3.0, 0.0, 0.0, 0.0, 0.0, -3.0, -2.0, 0.0, 126.0, -63.0, -27.0, -55.0, -5.0, 0.0, 1.0, 2.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=661,670 ) /
            -3.0, 28.0, 15.0, 2.0, 5.0, 0.0, 1.0, -2.0, 0.0, 9.0, 4.0, 1.0, 0.0, 9.0, 4.0, -1.0, -126.0, -63.0, -27.0, 55.0, 3.0, 0.0, 0.0, -1.0, 21.0, -11.0, -6.0, -11.0, 0.0, -4.0, 0.0, 0.0, -21.0, -11.0, -6.0, 11.0, -3.0, 0.0, 0.0, 1.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=671,680 ) /
            0.0, 3.0, 1.0, 0.0, 8.0, 0.0, 0.0, -4.0, -6.0, 0.0, 0.0, 3.0, -3.0, 0.0, 0.0, 1.0, 3.0, 0.0, 0.0, -1.0, -3.0, 0.0, 0.0, 1.0, -5.0, 0.0, 0.0, 2.0, 24.0, -12.0, -5.0, -11.0, 0.0, 3.0, 1.0, 0.0, 0.0, 3.0, 1.0, 0.0,
            // DATA ( ( ICPL(I,J) / I=1,4 ) / J=681,687 ) /
            0.0, 3.0, 2.0, 0.0, -24.0, -12.0, -5.0, 10.0, 4.0, 0.0, -1.0, -2.0, 13.0, 0.0, 0.0, -6.0, 7.0, 0.0, 0.0, -3.0, 3.0, 0.0, 0.0, -1.0, 3.0, 0.0, 0.0, -1.0)
}

