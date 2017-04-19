package com.gu.mobile.notifications.football.notificationbuilders

import com.gu.mobile.notifications.client.models.Importance.Major
import com.gu.mobile.notifications.client.models._
import com.gu.mobile.notifications.football.models.{Goal, GoalContext, Score}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import pa.{Competition, MatchDay, MatchDayTeam, Round, Stage, Venue}
import java.net.URI
import java.util.UUID


class MatchStatusNotificationBuilderSpec extends Specification {

  "A MatchStatusNotificationBuilder" should {

    "Build a notification" in new MatchEventsContext {
      builder.build(baseGoal, matchInfo, List.empty) shouldEqual FootballMatchStatusPayload(
        title = "Goal!",
        message = "Liverpool 1-0 Plymouth (HT)",
        thumbnailUrl = None,
        sender = "mobile-notifications-football-lambda",
        awayTeamName = "Plymouth",
        awayTeamId = "2",
        awayTeamScore = 0,
        awayTeamMessage = " ",
        homeTeamName = "Liverpool",
        homeTeamId = "1",
        homeTeamScore = 1,
        homeTeamMessage = "Steve 5'",
        matchId = "some-match-id",
        mapiUrl = new URI("http://localhost/sport/football/matches/some-match-id"),
        importance = Major,
        topic = Set(Topic(TopicTypes.FootballTeam, "1"), Topic(TopicTypes.FootballTeam, "2"), Topic(TopicTypes.FootballMatch, "some-match-id")),
        eventId = UUID.nameUUIDFromBytes((baseGoal :: List.empty).toString.getBytes).toString,
        phase = "HT",
        debug = false,
        competitionName = Some("FA Cup"),
        venue = Some("Wembley")
      )
    }

  }

  trait MatchEventsContext extends Scope {
    val goalTypes = List(OwnGoalType, PenaltyGoalType, DefaultGoalType)
    val builder = new MatchStatusNotificationBuilder("http://localhost")
    val home = MatchDayTeam("1", "Liverpool", None, None, None, None)
    val away = MatchDayTeam("2", "Plymouth", None, None, None, None)
    val baseGoal = Goal(DefaultGoalType, "Steve", home, away, 5, None)
    val goalContext = GoalContext(home, away, "match-1", Score(2, 0))
    val matchInfo = MatchDay(
      id = "some-match-id",
      date = DateTime.parse("2000-01-01T00:00:00Z"),
      competition = Some(Competition(id = "1", name = "FA Cup")),
      stage = Stage("1"),
      round = Round("1", None),
      leg = "home",
      liveMatch = true,
      result =  false,
      previewAvailable = false,
      reportAvailable = false,
      lineupsAvailable = false,
      matchStatus = "HT",
      attendance = None,
      homeTeam = home,
      awayTeam = away,
      referee = None,
      venue = Some(Venue(id = "1", name = "Wembley")),
      comments = None
    )
  }
}
