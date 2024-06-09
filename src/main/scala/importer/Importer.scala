package importer

import cats.data.ValidatedNec
import cats.effect._
import cats.implicits._
import importer.ImporterDefinition.ReportImporter
import importer.Model.ReportRowRepresentation
import org.http4s.Response
import org.http4s.dsl.io._
import persistence.DbClient
import persistence.Model.Event
import scodec.bits.ByteVector

import scala.jdk.StreamConverters._

object Importer {

  def process(request: ByteVector, importer: ReportImporter[ReportRowRepresentation])
             (rowsToEventsFunction: List[ReportRowRepresentation] => ValidatedNec[String, List[Event]]): IO[Response[IO]] = {
    request.decodeString(importer.charset)
      .fold(
        error => BadRequest(error.getMessage),
        fileContent => toRowRepresentations(fileContent, importer).toEither
          .flatMap(rows => rowsToEventsFunction(rows).toEither)
          .map(events => DbClient.saveAllEvents(events).flatMap(_ => Ok("Report imported.")))
          .leftMap(e => BadRequest(e.mkString_("\n")))
          .fold(x => x, x => x)
      )
  }

  private def toRowRepresentations(fileContent: String, importer: ReportImporter[ReportRowRepresentation]): ValidatedNec[String, List[ReportRowRepresentation]] = {
    fileContent.lines().toScala(LazyList)
      .drop(1)
      .filter(line => !line.isBlank)
      .map(importer.toReportRowRepresentation)
      .toList
      .sequence
  }

}
