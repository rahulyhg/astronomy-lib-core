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

package net.arwix.astronomy.core.vector

import java.lang.Math.*


abstract class Vector {

    companion object {
        internal fun convert(vector: Vector, toType: VectorType): Vector {
            when (vector.getType()) {
                VectorType.SPHERICAL -> {
                    if (toType == VectorType.SPHERICAL) return SphericalVector(vector)
                    vector as SphericalVector
                    val cosEl = Math.cos(vector.theta)
                    return RectangularVector(
                            vector.r * cos(vector.phi) * cosEl,
                            vector.r * sin(vector.phi) * cosEl,
                            vector.r * sin(vector.theta))
                }
                VectorType.RECTANGULAR -> {
                    if (toType === VectorType.RECTANGULAR) return RectangularVector(vector)
                    vector as RectangularVector
                    val sphericalVector = SphericalVector()
                    // Длина проекции на плоскость XY
                    val XYSqr = vector.x * vector.x + vector.y * vector.y
                    // Модуль вектора
                    sphericalVector.r = sqrt(XYSqr + vector.z * vector.z)
                    // Азимут вектора
                    sphericalVector.phi = if (vector.x == 0.0 && vector.y == 0.0) 0.0 else atan2(vector.y, vector.x)
                    if (sphericalVector.phi < 0.0) sphericalVector.phi += 2.0 * Math.PI
                    // высота вектора
                    val rho = sqrt(XYSqr)
                    sphericalVector.theta = if (vector.z == 0.0 && rho == 0.0) 0.0 else atan2(vector.z, rho)
                    return sphericalVector
                }
            }
        }

        /**
         * Скалярное перемножение векторов
         * @param left  вектор
         * @param right вектор
         * @return скалярное произведение
         */
        private fun dot(left: Vector, right: Vector): Double {
            val v1 = left.toType<RectangularVector>()
            val v2 = right.toType<RectangularVector>()
            return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z
        }

        /**
         * нормализатор вектора
         * @param vector вектор для нормализации
         * @return модуль вектора
         */
        private fun normalize(vector: Vector): Double {
            return sqrt(dot(vector, vector))
        }


        /**
         * Сложение координат
         * @param left  вектор
         * @param right вектор
         * @return новый вектор
         */
        private fun plus(left: Vector, right: Vector): RectangularVector {
            val v1 = left.toType<RectangularVector>()
            val v2 = right.toType<RectangularVector>()
            return RectangularVector(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z)
        }

        /**
         * Вычитание координат
         * @param left  вектор
         * @param right вектор
         * @return новый вектор
         */
        private fun minus(left: Vector, right: Vector): RectangularVector {
            val v1 = left.toType<RectangularVector>()
            val v2 = right.toType<RectangularVector>()
            return RectangularVector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z)
        }

        /**
         * Скалярное умножение
         * @param vector  вектор
         * @param scalar множитель
         * @return новый вектор
         */
        private fun scalarTimes(vector: Vector, scalar: Double): RectangularVector {
            val v = vector.toType<RectangularVector>()
            return RectangularVector(v.x * scalar, v.y * scalar, v.z * scalar)
        }

        /**
         * Скалярное деление
         * @param vector  вектор
         * @param scalar делитель
         * @return новый вектор
         */
        private fun scalarDivide(vector: Vector, scalar: Double) = scalarTimes(vector, 1.0 / scalar)


        /**
         * Унарный минус
         * @param vector исходный вектор
         * @return новый вектор
         */
        private fun unaryMinus(vector: Vector): Vector {
            val v = vector.toType<RectangularVector>()
            return RectangularVector(-v.x, -v.y, -v.z)
        }

        /**
         * Векторное произведение
         * @param left  вектор
         * @param right вектор
         * @return новый объект
         */
        private fun multiply(left: Vector, right: Vector): RectangularVector {
            val v1 = left.toType<RectangularVector>()
            val v2 = right.toType<RectangularVector>()
            val r = RectangularVector(
                    v1.y * v2.z - v1.z * v2.y,
                    v1.z * v2.x - v1.x * v2.z,
                    v1.x * v2.y - v1.y * v2.x
            )
            return r
        }

    }

    abstract fun toArray(): DoubleArray

    abstract fun getType(): VectorType

    abstract fun set(vector: Vector)

    abstract operator fun get(index: Int): Double

    @Deprecated("use toType", ReplaceWith("getVectorOfType(type: VectorType)", "toType()"))
    fun getVectorOfType(type: VectorType) = if (this.getType() == type) this else convert(this, type)

    inline fun <reified T : Vector> toType(): T {
        if (this is T) return this else {
            when (this) {
                is SphericalVector -> {
                    val cosEl = Math.cos(theta)
                    RectangularVector(
                            r * cos(phi) * cosEl,
                            r * sin(phi) * cosEl,
                            r * sin(theta))
                            .let {
                                if (it is T) return it
                                else throw Exception("Internal convert vector exception: ${T::class.java.canonicalName} is not RectangularVector")
                            }
                }
                is RectangularVector -> {
                    val sphericalVector = SphericalVector()
                    // Длина проекции на плоскость XY
                    val XYSqr = x * x + y * y
                    // Модуль вектора
                    sphericalVector.r = sqrt(XYSqr + z * z)
                    // Азимут вектора
                    sphericalVector.phi = if (x == 0.0 && y == 0.0) 0.0 else atan2(y, x)
                    if (sphericalVector.phi < 0.0) sphericalVector.phi += 2.0 * Math.PI
                    // высота вектора
                    val rho = sqrt(XYSqr)
                    sphericalVector.theta = if (z == 0.0 && rho == 0.0) 0.0 else atan2(z, rho)
                    if (sphericalVector is T) return sphericalVector
                    else throw Exception("Internal convert vector exception: ${T::class.java.canonicalName} is not SphericalVector")
                }

                else -> throw Exception("Internal convert vector exception: ${T::class.java.canonicalName} is unknown vector type")
            }
        }
    }


    abstract operator fun set(i: Int, element: Double)

    operator fun unaryMinus() = Companion.unaryMinus(this)

    operator fun plus(vector: Vector) = plus(this, vector)

    operator fun minus(vector: Vector) = minus(this, vector)

    operator fun times(scalar: Double) = scalarTimes(this, scalar)

    operator fun times(vector: Vector) = multiply(this, vector)

    operator fun times(right: Matrix) = Matrix.Companion.timesVM(this, right)

    operator fun div(scalar: Double) = scalarDivide(this, scalar)

    infix fun dot(vector: Vector): Double = dot(this, vector)

    fun normalize() = normalize(this)

    operator fun plusAssign(vector: Vector) {
        set(this + vector)
    }

    operator fun minusAssign(vector: Vector) {
        set(this - vector)
    }

    operator fun timesAssign(scalar: Double) {
        set(this * scalar)
    }

    operator fun timesAssign(vector: Vector) {
        set(this * vector)
    }

    operator fun timesAssign(matrix: Matrix) {
        set(this * matrix)
    }

    operator fun divAssign(scalar: Double) {
        set(this / scalar)
    }

    abstract operator fun component1(): Double

    abstract operator fun component2(): Double

    abstract operator fun component3(): Double

}

