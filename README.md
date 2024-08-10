# HwhatsApp
Welcome to my bootleg WhatsApp, HwhatsApp!
 
This is a simple terminal based chat service that allows for users to communicate
with others on the same server, written entirely in Java. It supports chatting, account
handling, preserves data across sessions and permission limits.
 
To run, simply run the server class on wherever you want to run your server, and then
open your terminal and run the HwhatsApp.java file. 
 
Upon startup, you will be prompted to enter a userID. If you haven't used the service since
starting your server, you will also need to input a username. Then, you can create a chat and others
to it or be added to a chat by friends to start communicating. At any point you can switch accounts
as long as you know the userID of the other user (i.e. your password). The logout command stops the
instance of your program.
### Demo 
[Demo](https://www.loom.com/embed/ef6e74570c5645e8b390e8989db26721?sid=91379507-e3da-4e4e-a9ed-e7f2b86b9ae1)
 
### Features to come:
1) Permission removals by chat owner
2) Support for other message types, not just strings
3) Fixing some of the common concurreny bugs like chats not updating in real time sometimes
 
Code was written 100% by me for fun, no tutorials etc so if there is bug then it's definitely expected.
