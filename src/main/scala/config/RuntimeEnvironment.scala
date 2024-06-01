//package config
//
//import cats.data.{Kleisli, ReaderT}
//import cats.effect.IO
//import importer.Importer.{ExanteImporter, LiveExanteImporter, LiveXTBImporter, XTBImporter}
//
//object RuntimeEnvironment {
//
//  type Environment = XTBImporter with ExanteImporter
//  val liveEnvironment: Environment = new LiveXTBImporter with LiveExanteImporter
//
//  def readEnv: Kleisli[IO, Environment, Environment] = ReaderT.ask[IO, Environment]
//
//}
