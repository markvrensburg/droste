package qq.droste

import cats.Functor
import cats.instances.either._
import cats.instances.tuple._
import cats.syntax.functor._

import data.prelude._
import data.Cofree
import data.Free
import syntax.alias._

private[droste] trait Zoo {

  /** A variation of an anamorphism that lets you terminate any point of
    * the recursion using a value of the original input type.
    *
    * One use case is to return cached/precomputed results during an
    * unfold.
    *
    * @group unfolds
    *
    * @usecase def apo[F[_], A, R](coalgebra: RCoalgebra[R, F, A]): A => R
    *   @inheritdoc
    */
  def apo[F[_]: Functor, A, R](
    coalgebra: RCoalgebra[R, F, A]
  )(implicit embed: Embed[F, R]): A => R =
    scheme.hyloC(
      embed.algebra.run.compose((frr: F[(R | R)]) => frr.map(_.merge)),
      coalgebra.run)

  /** A variation of a catamorphism that gives you access to the input value at
    * every point in the computation.
    *
    * A paramorphism "eats its argument and keeps it too.
    *
    * This means each step has access to both the computed result
    * value as well as the original value.
    *
    * @group folds
    *
    * @usecase def para[F[_], R, B](algebra: RAlgebra[R, F, B]): R => B
    *   @inheritdoc
    */
  def para[F[_]: Functor, R, B](
    algebra: RAlgebra[R, F, B]
  )(implicit project: Project[F, R]): R => B =
    scheme.hyloC(
      algebra.run,
      project.coalgebra.run.andThen(_.map(r => (r, r))))


  /** Histomorphism
    *
    * @group folds
    *
    * @usecase def histo[F[_], R, B](algebra: CVAlgebra[F, B]): R => B
    *   @inheritdoc
    */
  def histo[F[_]: Functor, R, B](
    algebra: CVAlgebra[F, B]
  )(implicit project: Project[F, R]): R => B =
    scheme.hylo[F, R, Cofree[F, B]](
      fb => Cofree(algebra(fb), fb),
      project.coalgebra.run
    ) andThen (_.head)

  /** Futumorphism
    *
    * @group unfolds
    *
    * @usecase def futu[F[_], A, R](coalgebra: CVCoalgebra[F, A]): A => R
    *   @inheritdoc
    */
  def futu[F[_]: Functor, A, R](
    coalgebra: CVCoalgebra[F, A]
  )(implicit embed: Embed[F, R]): A => R =
    scheme.hylo[F, Free[F, A], R](
      embed.algebra.run,
      _.fold(coalgebra.run, identity)
    ) compose (Free.pure(_))

  /** A fusion refold of a futumorphism followed by a histomorphism
    *
    * @group refolds
    *
    * @usecase def chrono[F[_], A, B](algebra: CVAlgebra[F, B], coalgebra: CVCoalgebra[F, A]): A => B
    *   @inheritdoc
    */
  def chrono[F[_]: Functor, A, B](
    algebra: CVAlgebra[F, B],
    coalgebra: CVCoalgebra[F, A]
  ): A => B =
    scheme.hylo[F, Free[F, A], Cofree[F, B]](
      fb => Cofree(algebra(fb), fb),
      _.fold(coalgebra.run, identity)
    ) andThen (_.head) compose (Free.pure(_))

  /** A fusion refold of an anamorphism followed by a histomorphism
    *
    * @group refolds
    *
    * @usecase def dyna[F[_], A, B](algebra: CVAlgebra[F, B], coalgebra: Coalgebra[F, A]): A => B
    *   @inheritdoc
    */
  def dyna[F[_]: Functor, A, B](
    algebra: CVAlgebra[F, B],
    coalgebra: Coalgebra[F, A]
  ): A => B =
    scheme.hylo[F, A, Cofree[F, B]](
      fb => Cofree(algebra(fb), fb),
      coalgebra.run
    ) andThen (_.head)
}
