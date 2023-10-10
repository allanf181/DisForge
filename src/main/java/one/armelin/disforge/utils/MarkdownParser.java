package one.armelin.disforge.utils;

import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser {

    public static String parseMarkdown(String message ) {
        String translated = message;

        translated = replaceWith( translated, "(?<!\\\\)\\*\\*", ChatFormatting.BOLD.toString(), ChatFormatting.RESET.toString() );
        translated = replaceWith( translated, "(?<!\\\\)\\*", ChatFormatting.ITALIC.toString(), ChatFormatting.RESET.toString() );
        translated = replaceWith( translated, "(?<!\\\\)__", ChatFormatting.UNDERLINE.toString(), ChatFormatting.RESET.toString() );
        translated = replaceWith( translated, "(?<!\\\\)_", ChatFormatting.ITALIC.toString(), ChatFormatting.RESET.toString() );
        translated = replaceWith( translated, "(?<!\\\\)~~", ChatFormatting.STRIKETHROUGH.toString(), ChatFormatting.RESET.toString() );

        translated = translated.replaceAll( "\\*", "*" ).replaceAll( "\\_", "_" ).replaceAll( "\\~", "~" );
        translated = translated.replaceAll(ChatFormatting.ITALIC.toString()+"(ツ)"+ChatFormatting.RESET.toString(),"_(ツ)_");
        return translated;
    }

    private static String replaceWith( String message, String quot, String pre, String suf ) {
        String part = message;
        for ( String str : getMatches( message, quot + "(.+?)" + quot ) ) {
            part = part.replaceFirst( quot + Pattern.quote( str ) + quot, pre + str + suf );
        }
        return part;
    }

    public static List< String > getMatches(String string, String regex ) {
        Pattern pattern = Pattern.compile( regex );
        Matcher matcher = pattern.matcher( string );
        List< String > matches = new ArrayList<>();
        while ( matcher.find() ) {
            matches.add( matcher.group( 1 ) );
        }
        return matches;
    }


}
