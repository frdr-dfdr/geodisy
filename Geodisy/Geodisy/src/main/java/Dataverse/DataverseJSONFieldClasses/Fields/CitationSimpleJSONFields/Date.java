package Dataverse.DataverseJSONFieldClasses.Fields.CitationSimpleJSONFields;


import Dataverse.DataverseJSONFieldClasses.JSONField;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

/**
 * This class makes sure that the date entered as a string is actually formatted correctly and if it is not it returns
 * a date with the year 9999. When getting a date, if the date is year 9999, it returns an empty string.
 */
public class Date extends JSONField implements DateField {
    TemporalAccessor date;

    public Date(String date) {
       try {
           this.date = checkDateString(date);
       }catch (DateTimeParseException e){
           this.date = checkDateString("9999");
       }

    }

    public TemporalAccessor getDate() {
        return date;
    }

    public String getDateAsString(){
        if(date==null ||date.toString()=="9999")
            return "";
        return date.toString();
    }

    @Override
    public String getField(String fieldName) {
        return getDateAsString();
    }

    public static TemporalAccessor checkDateString(String value) throws DateTimeParseException{
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[uuuu[-MM[-dd['T'HH:mm:ss[X]]]]");
        return formatter.parseBest(value, ZonedDateTime::from, LocalDateTime::from, LocalDate::from, YearMonth::from, Year::from);
    }

    public int getYear(){
        try{
            Year y = Year.from(date);
            if(y.toString()!="9999")
                return y.getValue();
        } catch (DateTimeException e){
            //value -111111 is to indicate there was not a parsable date to get a year value from this TemporalAccessor
            return -111111;
        }
        return -111111;
    }


}
