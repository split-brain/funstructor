# Help

Lot of people get confused when playing Funstructor for the first time, so we've made
a quick game tutorial that helps you understand the basics.

**Note:** to play you must love clojure and card games

## Connecting

Open the [funstructor.clojurecup.com](http://funstructor.clojurecup.com), type your name and press play.

You may have to wait for another player to connect. Ask your friend to play with you or open the game in a separate browser tab and play with yourself.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/enter_the_game.png"
     width="600"
	 height="320" />

When you found an opponent to play with, the game begins and you see a *game screen*

## Game

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/start_game.png"
     width="600"
	 height="320" />

The game is like a programming, your **goal** is to make some clojure code, either function or expression.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/goal.png"
     width="251"
	 height="171" />

Your opponent that you will play against has the same goal but he will be assigned a different task. Players don't know what task their opponent has. Instead of programming, where you type characters, in this game you **play cards** to make code.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/your_cards.png"
     width="600"
	 height="116" />

Obviously, you can't see opponent's cards.

This is your editor where you can put your cards, we call it a **board**

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/board.png"
     width="600"
	 height="63" />

There are yellow lines on the board - **gaps**, empty spaces where you can put some tokens. In order to add more gaps, use **red** cards: `Gap`, `Left Gap` or `Right Gap`

To use `Left Gap` and `Right Gap` just drag and drop them on your board.
As a result, a new gap will be added to the leftmost or rightmost position, respectively.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/right_gap.png"
     width="600"
	 height="180" />

`Gap` card is very useful, because you can specify the position for the gap.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/pos_gap.png"
     width="600"
	 height="180" />

The problem is that the drop area is very small (*we are working on the usability!*) So get the hand to use it.

But gaps are just gaps, you need some code!

In order to make code, use **blue** cards - **terminals**. These are building blocks for your function.
By the way, we call list of elements that you've put so far - **funstruct**, cute name. Start building your function by drag and dropping terminals to the highlighted gap.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/left_paren.png"
     width="600"
	 height="180" />

Some terminals need input, like `ID` or `NUM`. Play them the same way as other terminals, but add a value afterwards.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/id_play.png"
     width="600"
	 height="180" />

**Note:** Identifiers in clojure could contain a lot of symbols: `?`, `!`, `-`. Even more: `+` and `*` are identifiers!

After you've played all cards you can or you want, finish your turn by pressing the "End turn" button. Now the opponent plays. His view is below.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/opponents_view.png"
     width="600"
	 height="320" />

Few important things to note here. Opponent can't see your cards and can't see your goal. But! He can see *funstruct* that you've build. He also can inspect game log to see what've you played on the last turn.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/game_log.png"
     width="200"
	 height="300" />

Game log is useful because you can also use it as a chat with an opponent.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/chat.png"
     width="200"
	 height="300" />

Sound notifications wake up opponent if he forgot to end a turn.

After both players end their turns, new game turn starts. Each player gets two cards from the deck to the hand, and third card goes to a random player. Pay attention that third card appears in the log, so you can make assumptions what cards are in opponent's hand.

Turn by turn players build their funstructs, and who the one who managed to achieve his goal first - wins.

There are also a bunch of interesting card types like **Durations** or **Actions** that adds new functionality, investigate them by playing.

<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/tutorial/more_cards.png"
     width="600"
	 height="124" />

Don't hesitate to leave feedback, suggest new cards and vote for us!

[<img src="https://github.com/clojurecup2014/funstructor/blob/master/doc/img/vote.jpeg">](https://clojurecup.com/#/apps/funstructor)
