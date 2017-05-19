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

package net.arwix.astronomy.calculator

//import net.arwix.astronomy.vsop87.CEarthData
import net.arwix.astronomy.annotation.Ecliptic
import net.arwix.astronomy.annotation.Geocentric
import net.arwix.astronomy.annotation.Heliocentric
import net.arwix.astronomy.core.C_Light
import net.arwix.astronomy.core.DEG
import net.arwix.astronomy.core.Position
import net.arwix.astronomy.core.calendar.getJT
import net.arwix.astronomy.core.kepler.KeplerianOrbit
import net.arwix.astronomy.core.vector.RectangularVector
import net.arwix.astronomy.core.vector.SphericalVector
import net.arwix.astronomy.core.vector.Vector
import net.arwix.astronomy.core.vector.VectorType
import net.arwix.astronomy.ephemeris.precession.Precession
import net.arwix.astronomy.vsop87.*
import org.junit.Test
import java.util.*


class RiseSetCalculatorTest {
    @Test
    fun calls() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        calendar.set(Calendar.YEAR, 2014)
        calendar.set(Calendar.MONTH, 8)
        calendar.set(Calendar.DAY_OF_MONTH, 17)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val t = calendar.getJT(true)

        // J2000
        val positionA = Position(AEarthData() as VsopData)
        @Heliocentric @Ecliptic var earthEcliptic = (AEarthData() as VsopData).getEclipticCoordinates(t)
        @Heliocentric @Ecliptic var objEcliptic = (AMercuryData() as VsopData).getEclipticCoordinates(t)
        @Geocentric @Ecliptic var objGeoEcliptic: Vector = objEcliptic - earthEcliptic
        val dT = objGeoEcliptic.normalize() / C_Light / 36525.0
        objEcliptic = (AMercuryData() as VsopData).getEclipticCoordinates(t - dT)
        objGeoEcliptic = objEcliptic - earthEcliptic


        val precession = Precession.JPL_DE4xx(t)
        val nutation = precession.getNearsetNutation()
        val obliquity = precession.getNearestObliquity()

        objGeoEcliptic = precession.transformFromJ2000(objGeoEcliptic)
//        objGeoEcliptic = PrecessionMethod.JPL_DE4xx.transformPrec(t, objGeoEcliptic)

        val lightTime = dT * 36525.0


        val orbit = KeplerianOrbit.Planet.MERCURY.getOrbitalPlane(t)
        val eOrbit = KeplerianOrbit.Planet.EARTH.getOrbitalPlane(t)

        //     objGeoEcliptic = objGeoEcliptic - (orbit.velocity - eOrbit.velocity).times(dT * 36525.0)

        objGeoEcliptic = aberration(objGeoEcliptic.getVectorOfType(VectorType.RECTANGULAR) as RectangularVector, eOrbit.velocity as RectangularVector, lightTime)

        //      objGeoEcliptic = (PrecessionMethod.WILLIAMS_1994.getEclipticPrecessionVector(t, true) * objGeoEcliptic ).getVectorOfType(VectorType.RECTANGULAR) as RectangularVector

        //     objGeoEcliptic = PrecessionMethod.JPL_DE4xx.transformPrec(t, objGeoEcliptic)
        //eclipticToEquatorial
//        objGeoEcliptic = net.arwix.astronomy.ephemeris.precession.Nutation.FastNutation(t).removeEclipticNutation(objGeoEcliptic)

//        val epsilon = Obliquity.meanObliquity(t)
//        val m = Matrix.transpose(Matrix(Matrix.Axis.X, epsilon))
        var vectorAltA = obliquity.rotateFromEclipticToEquatorial(objGeoEcliptic)
        //     var vectorAltA = m * objGeoEcliptic

        //    vectorAltA = Precession.precessFromJ2000(t, vectorAltA)
        //   vectorAltA = PrecessionMethod.JPL_DE4xx.transformPrec(t, vectorAltA)
        //    vectorAltA = Nutation.nutateInEquatorialCoordinates(t, true, vectorAltA, true)

        //   vectorAltA = nutateInEquatorial111Coordinates(t, vectorAltA, false)

        vectorAltA = nutation.applyNutationToGeocentricVector(vectorAltA)

        //   System.out.println("e= " + epsilon.toString())
        System.out.println("Alt J2000 " + printLong(vectorAltA))
        System.out.println("Alt J2000 " + printLat(vectorAltA))


        var vectorA = positionA.getGeocentricEquatorialPosition(t, AMercuryData() as VsopData)
        vectorA = net.arwix.astronomy.ephem.Precession.precessFromJ2000(t, vectorA)
        System.out.println(printLong(vectorA))
        System.out.println(printLat(vectorA))

        // Apparent
        val eD: VsopData = CEarthData()
        val position = Position(eD)
        val vector = position.getGeocentricEquatorialPosition(t, CMercuryData() as VsopData)
        System.out.println(printLong(vector))
        System.out.println(printLat(vector))





//
//        val c = RiseSetCalculator(date, location, { t: Double, e: Epoch ->
//
//            Position(CEarthData()).getGeocentricEquatorialPosition(t, object : EclipticCoordinates<Any> {
//                override fun getEpoch(): Epoch {
//                    return e
//                }
//
//                override fun getIdObject(): Any {
//                    return "Sun"
//                }
//
//                override fun getEclipticCoordinates(T: Double): Vector {
//                    return RectangularVector(0.0, 0.0, 0.0)
//                }
//
//            })
//
//        }, RiseSetCalculator.ObjectType.SUN)
//
//        c.getResult().let {
//            when (it) {
//                is RiseSetCalculator.Result.RiseSet -> {
//                    printResult(it.set.calendar.timeInMillis)
//                }
//            }
//        }

    }

    fun printResult(tMill: Long) {
        val calendar = Calendar.getInstance()

        System.out.println("EVENTS - " +
                calendar.apply { this.timeInMillis = tMill }.time.toString() + "; ")

    }

    fun aberration(pObject: RectangularVector, vearth: RectangularVector, light_time: Double): RectangularVector {
        if (light_time <= 0) return pObject

        //    val vearth = doubleArrayOf(earth[3], earth[4], earth[5])
        val p = DoubleArray(3)

        val TL = light_time
        val P1MAG = TL * C_Light
        val VEMAG = vearth.normalize()
        if (VEMAG == 0.0) return pObject
        val BETA = VEMAG / C_Light
        val DOT = pObject[0] * vearth.x + pObject[1] * vearth.y + pObject[2] * vearth.z
        val COSD = DOT / (P1MAG * VEMAG)
        val GAMMAI = Math.sqrt(1.0 - BETA * BETA)
        val P = BETA * COSD
        val Q = (1.0 + P / (1.0 + GAMMAI)) * TL
        val R = 1.0 + P

        for (i in 0..2) {
            p[i] = (GAMMAI * pObject[i] + Q * vearth[i]) / R
        }

        return RectangularVector(p[0], p[1], p[2])
    }


    private fun printLong(p: Vector): String {
        val vector = p.getVectorOfType(VectorType.SPHERICAL) as SphericalVector

        val hours = DEG * vector.phi / 15.0

        val hour = hours.toInt()
        val minutes = (hours - hour) * 60.0
        val minute = minutes.toInt()
        val seconds = (minutes - minute) * 60.0

        return String.format(Locale.ENGLISH, "%1$02d:%2$02d:%3$.2f", hour, minute, seconds)
    }

    private fun printLat(p: Vector): String {
        val vector = p.getVectorOfType(VectorType.SPHERICAL) as SphericalVector

        val g = Math.toDegrees(vector.theta).toInt()
        val mm = (Math.toDegrees(vector.theta) - g) * 60.0
        val m = mm.toInt()
        val s = (mm - m) * 60.0
        return String.format(Locale.ENGLISH, "%1$02d %2$02d %3$.1f", g, Math.abs(m), Math.abs(s))
    }
}
