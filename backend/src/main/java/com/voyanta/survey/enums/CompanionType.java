package com.voyanta.survey.enums;

/**
 * Enum telling who the user travels with. Choosing FAMILY is what makes the survey ask
 * for FamilyDetails, and the value is passed on to the AI and saved on the travel plan.
 */
public enum CompanionType {
    SOLO, COUPLE, FRIENDS, FAMILY
}