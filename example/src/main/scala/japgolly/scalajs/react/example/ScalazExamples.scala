package japgolly.scalajs.react.example

import org.scalajs.dom.Node
import scalaz.effect.IO

import japgolly.scalajs.react._
import vdom.ReactVDom._
import all._
import ScalazReact._

object ScalazExamples {

  /**
   * This is the same as ReactExamples.example3, just modified for more serious FP/Scalaz.
   */
  def example3(mountNode: Node) = {

    val TodoList = ReactComponentB[List[String]]("TodoList")
      .render(P => {
        def createItem(itemText: String) = li(itemText)
        ul(P map createItem)
      })
      .create

    case class State(items: List[String], text: String)

    val ST = ReactS.Fix[State]                           // Let's use a helper so that we don't have to specify the
                                                         //   state type everywhere.

    def acceptChange(e: InputEvent) =
      ST.mod(_.copy(text = e.target.value))              // A pure state modification. State value is provided later.

    def handleSubmit(e: InputEvent) = (
      ST.retM(e.preventDefaultIO)                        // Lift an IO effect into a shape that allows composition
                                                         //   with state modification.
      >>                                                 // Use >> to compose. It's flatMap (>>=) that ignores input.
      ST.mod(s => State(s.items :+ s.text, "")).lift[IO] // Here we lift a pure state modification into a shape that
    )                                                    //   allows composition with IO effects.

    val TodoApp = ReactComponentB[Unit]("TodoApp")
      .initialState(State(Nil, ""))
      .renderS((T,_,S) =>                                // Using renderS instead of render to get T (`this` in JS).
        div(
          h3("TODO"),
          TodoList(S.items),
          form(onsubmit ~~> T._runState(handleSubmit))(  // In Scalaz mode, only use ~~> for callbacks.
            input(                                       //   ==> and --> are unsafe.
              onchange ~~> T._runState(acceptChange),    // runState runs a state monad and applies the result.
              value := S.text),                          // _runState takes an input first (in this case, InputEvent).
            button("Add #", S.items.length + 1)
          )
        )
      ).createU

    React.renderComponent(TodoApp(), mountNode)
  }
}
