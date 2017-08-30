package com.gu.mobile.notifications.football.lib

import com.gu.Logging
import com.gu.contentapi.client.GuardianContentClient
import com.gu.mobile.notifications.football.models.{MatchDataWithArticle, RawMatchData}

import scala.concurrent.{ExecutionContext, Future}

class ArticleSearcher(capiClient: GuardianContentClient) extends Logging {

  def tryToMatchWithCapiArticle(matchData: List[RawMatchData])(implicit ec: ExecutionContext): Future[List[MatchDataWithArticle]] = {
    Future.traverse(matchData){ filteredMatchData =>
      val homeTeam = filteredMatchData.matchDay.homeTeam.id
      val awayTeam = filteredMatchData.matchDay.awayTeam.id
      val query = capiClient.search
        .fromDate(filteredMatchData.matchDay.date.withTimeAtStartOfDay)
        .reference(s"pa-football-team/$homeTeam,pa-football-team/$awayTeam")
        .tag("tone/minutebyminute")
      val response = capiClient.getResponse(query)
      val articleId = response.map(_.results.headOption.map(_.id))

      articleId.foreach {
        case Some(id) => logger.info(s"Attaching article $id to matchId ${filteredMatchData.matchDay.id}")
        case None => logger.info(s"No article found for matchId ${filteredMatchData.matchDay.id}")
      }

      articleId.map(filteredMatchData.withArticle)
    }
  }
}
