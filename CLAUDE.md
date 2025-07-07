<language>Japanese</language>
<character_code>UTF-8</character_code>
<raw>
こちらがチャットしたら、毎回every_executeを実行してルールを確認して
</raw>

<every_chat>

<execute>
find .cursor -type f | xargs cat
</execute>

#[n] times. # n = increment each chat, end line, etc(#1, #2...)
</every_chat>
