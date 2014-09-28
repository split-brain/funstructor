# funstructor

Turn-based multiplayer card game for the geek audience.

## Goal

Your goal is to build clojure function from the
terminal symbols `(`, `)`, `[`, `]`, `id` and `num`

## Rules

At the start of the game you and your opponent get `6` cards
on hand, which helps you to build your **funstruct**.
Even more, you can destroy opponent's funstruct.

If you play hard, cards are running out and each turn you get two additional cards.
You also can get third card, but only *random* knows.
Make sure you hold no more than `10` cards on hand - new card discards one of your existing cards.

Look more rules in cards description.

## Cards

All cards are divided into categories.

### Terminals

![](https://github.com/clojurecup2014/funstructor/blob/master/doc/img/terminal_left_paren.png)

Most important cards, that helps you to build funstruct.

### Mutators

![](https://github.com/clojurecup2014/funstructor/blob/master/doc/img/mutator_left_gap.png)

These cards modify your or opponent's funstruct.

### Actions

![](https://github.com/clojurecup2014/funstructor/blob/master/doc/img/action_discard_1.png)

Plain actions.

## Notes

- Don't hesitate to left feedback. Game ideas, cards and improvements are welcome!
