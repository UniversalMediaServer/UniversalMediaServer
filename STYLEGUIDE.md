# Style Guide

## Git merging

There are a lot of different ways to merge branches, from pulling to stashing/popping to rebasing and more.

We prefer pulling. That is, to merge some code into your branch, pull from that branch into your branch.

The main reason for this is that it creates "merge commits" which makes it easier to see where code conflicts have occurred, which is especially useful when they have been incorrectly resolved, which can easily happen especially on an active project. The other strategies hide/rewrite that history.