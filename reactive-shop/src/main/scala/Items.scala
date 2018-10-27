import java.util.UUID

object Items {
  sealed trait Item

  final case class Apple(id: UUID = UUID.randomUUID()) extends Item
  final case class Orange(id: UUID = UUID.randomUUID()) extends Item
  final case class Watermelon(id: UUID = UUID.randomUUID()) extends Item

}
