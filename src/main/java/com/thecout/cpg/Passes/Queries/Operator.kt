package com.thecout.cpg.Passes.Queries

enum class Operator(val value: String) {
    EQUAL("="),
    NOT_EQUAL("!="),
    GREATER_THAN(">"),
    GREATER_THAN_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_EQUAL("<="),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    BETWEEN("BETWEEN"),
    NOT_BETWEEN("NOT BETWEEN"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    REGEX("=~")
}