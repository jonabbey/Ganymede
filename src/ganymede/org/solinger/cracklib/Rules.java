package org.solinger.cracklib;

public class Rules {
    
    public static final char RULE_NOOP = ':';
    public static final char RULE_PREPEND = '^';
    public static final char RULE_APPEND = '$';
    public static final char RULE_REVERSE = 'r';
    public static final char RULE_UPPERCASE = 'u';
    public static final char RULE_LOWERCASE = 'l';
    public static final char RULE_PLURALISE = 'p';
    public static final char RULE_CAPITALISE = 'c';
    public static final char RULE_DUPLICATE = 'd';
    public static final char RULE_REFLECT = 'f';
    public static final char RULE_SUBSTITUTE = 's';
    public static final char RULE_MATCH = '/';
    public static final char RULE_NOT = '!';
    public static final char RULE_LT = '<';
    public static final char RULE_GT = '>';
    public static final char RULE_EXTRACT = 'x';
    public static final char RULE_OVERSTRIKE = 'o';
    public static final char RULE_INSERT = 'i';
    public static final char RULE_EQUALS = '=';
    public static final char RULE_PURGE = '@';
    // class rule? socialist ethic in cracker? 
    public static final char RULE_CLASS = '?' ;
    
    public static final char RULE_DFIRST = '[';
    public static final char RULE_DLAST = ']';
    public static final char RULE_MFIRST = '(';
    public static final char RULE_MLAST = ')';
    
    public static final boolean suffix(String myword, String suffix) {
	int i = myword.length();
	int j = suffix.length();
	
	if (i > j) {
	    return myword.indexOf(suffix) == i-j;
	} else {
	    return false;
	}
    }

    public static final String reverse(String s) {
	return new StringBuffer(s).reverse().toString();
    }
    
    public static final String capitalise(String s) {
	return s.substring(0,1).toUpperCase()+s.substring(1).toLowerCase();
    }
    
    public static final String pluralise(String s) {
	if (suffix(s, "ch") || suffix(s, "ex") || suffix(s, "ix") ||
	    suffix(s, "sh") || suffix(s, "ss")) {
	    
	    /* bench -> benches */
	    return s+"es";
	} else if (s.length() > 2 && s.charAt(s.length()-1) == 'y') {
	    if ("aeiou".indexOf(s.charAt(s.length() - 2)) != -1) {
		/* alloy -> alloys */
		return s+"s";
	    } else {
		/* gully -> gullies */
		return s.substring(s.length()-2)+"ies";
	    }
	} else if (s.charAt(s.length()-1) == 's') {
	    /* bias -> biases */
	    return s+"es";
	} else {
	    /* catchall */
	    return s+"s";
	}
    }
  
    public static final String purge(String s, char c) {
	StringBuffer sb = new StringBuffer();
	for (int i=0;i<s.length();i++) {
	    if (s.charAt(i) != c) {
		sb.append(s.charAt(i));
	    }
	}
	return sb.toString();
    }
    
    /**
     * this function takes two inputs, a class identifier and a character, and
     * returns non-null if the given character is a member of the class, based
     * upon restrictions set out below
     */
    public static final boolean matchClass(char clazz, char c) {
      boolean retval = false;
    
	switch (clazz) {
	    /* ESCAPE */
	    
	case '?':			/* ?? -> ? */
	    if (c == '?') {
		retval = true;
	    }
	    break;
	    
	    /* ILLOGICAL GROUPINGS (ie: not in ctype.h) */
	    
	case 'V':
	case 'v':			/* vowels */
	  if ("aeiou".indexOf(Character.toLowerCase(c)) != -1) {
	    retval = true;
	  }
	  break;
	case 'C':
	case 'c':			/* consonants */
	  if ("bcdfghjklmnpqrstvwxyz".
	      indexOf(Character.toLowerCase(c)) != -1) {
	    retval = true;
	  }
	  break;
	case 'W':
	case 'w':			/* whitespace */
	  retval = Character.isWhitespace(c);
	  break;
	  
	case 'P':
	case 'p':			/* punctuation */
	  if (".`,:;'!?\"".indexOf(Character.toLowerCase(c)) != -1)  {
	    retval = true;
	  }
	  break;
	    
	case 'S':
	case 's':			/* symbols */
	  if ("$%%^&*()-_+=|\\[]{}#@/~"
	      .indexOf(Character.toLowerCase(c)) != -1) {
	    retval = true;
	  }
	  break;
	    
	  /* LOGICAL GROUPINGS */
	    
	case 'L':
	case 'l':			/* lowercase */
	  retval = Character.isLowerCase(c);
	  break;
	    
	case 'U':
	case 'u':			/* uppercase */
	  retval = Character.isUpperCase(c);
	  break;
	    
	case 'A':
	case 'a':			/* alphabetic */
	  retval = Character.isLetter(c);
	  break;
	  
	case 'X':
	case 'x':			/* alphanumeric */
	  retval = Character.isLetterOrDigit(c);
	  break;
	    
	case 'D':
	case 'd':			/* digits */
	  retval = Character.isDigit(c);
	  break;
	    
	default:
	    throw new IllegalArgumentException
		("MatchClass: unknown class :"+clazz);
	}
	
	if (Character.isUpperCase(clazz)) {
	  return retval;
	}
	return retval;
    }

    public static final int indexOf(String s, char clazz) {
	for (int i=0;i<s.length();i++) {
	    if (matchClass(clazz,s.charAt(i))) {
		return i;
	    }
	}
	return -1;
    }

    public static final String replace(String s, char clazz, char newChar) {
	StringBuffer sb = new StringBuffer();
	for (int i=0;i<s.length();i++) {
	    if (matchClass(clazz,s.charAt(i))) {
		sb.append(newChar);
	    } else {
		sb.append(s.charAt(i));
	    }
	}
	return sb.toString();
    }


    public static final String polyPurge(String s, char clazz) {
	StringBuffer sb = new StringBuffer();
	for (int i=0;i<s.length();i++) {
	    if (!matchClass(clazz,s.charAt(i))) {
		sb.append(s.charAt(i));
	    }
	}
	return sb.toString();
    }

    /** 
     * Silly function used for > and < compares.
     */
    public static final int char2Int(char c) {
	
	if (Character.isDigit(c)) {
	    return (c - '0');
	} else if (Character.isLowerCase(c)) { 
	    return (c - 'a' + 10);
	} else if (Character.isUpperCase(c)) {
	    return (c - 'A' + 10);
	}
	return -1;
    }

    public static final String mangle(String s, String control) {
	
	for (int i=0;i<control.length();i++) {
	    switch (control.charAt(i)) {
	    case RULE_NOOP:
		break;
	    case RULE_REVERSE:
		s = reverse(s);
		break;
	    case RULE_UPPERCASE:
		s = s.toUpperCase();
		break;
	    case RULE_LOWERCASE:
		s = s.toLowerCase();
		break;
	    case RULE_CAPITALISE:
		s = capitalise(s);
		break;
	    case RULE_PLURALISE:
		s = pluralise(s);
		break;
	    case RULE_REFLECT:
		s = reverse(s);
		break;
	    case RULE_DUPLICATE:
		s = s + s;
		break;
	    case RULE_GT:
		if (i == control.length()-1) {
		    throw new IllegalArgumentException
			("mangle: '>' missing argument in :"+control);
		} else {
		    int limit = char2Int(control.charAt(++i));
		    if (limit < 0) {
			throw new IllegalArgumentException
			    ("mangle: '>' weird argument in :"+control);
		    }
		    if (s.length() <= limit) {
			return null;
		    }
		}
		break;
	    case RULE_LT:
		if (i == control.length()-1) {
		    throw new IllegalArgumentException
			("mangle: '<' missing argument in :"+control);
		} else {
		    int limit = char2Int(control.charAt(++i));
		    if (limit < 0) {
			throw new IllegalArgumentException
			    ("mangle: '<' weird argument in :"+control);
		    }
		    if (s.length() >= limit) {
			return null;
		    }
		}
		break;
	    case RULE_PREPEND:
		if (i == control.length()-1) {
		    throw new IllegalArgumentException
			("mangle: prepend missing argument in :"+control);
		} else {
		    s = control.charAt(++i) + s;
		}
		break;
	    case RULE_APPEND:
		if (i == control.length()-1) {
		    throw new IllegalArgumentException
			("mangle: prepend missing argument in :"+control);
		} else {
		    s = s + control.charAt(++i);
		}
		break;
	    case RULE_EXTRACT:
		if (i >= control.length()-2) {
		    throw new IllegalArgumentException
			("mangle: extract missing argument in :"+control);    
		} else {
		    int start = char2Int(control.charAt(++i));
		    int length = char2Int(control.charAt(++i));
		    if (start < 0 || length < 0) {
			throw new IllegalArgumentException
			    ("mangle: extract: weird argument in :"+control);
		    }
		    s = s.substring(start,start+length);
		}
		break;
	    case RULE_OVERSTRIKE:
		if (i >= control.length()-2) {
		    throw new IllegalArgumentException
			("mangle: overstrike missing argument in :"+control);
		} else {
		    int pos = char2Int(control.charAt(++i));
		    if (i < 0) {
			throw new IllegalArgumentException
			    ("mangle: overstrike weird argument in :"+control);
		    } 
		    StringBuffer sb = new StringBuffer(s);
		    sb.setCharAt(pos,control.charAt(++i));
		    s = sb.toString();
		}
		break;
	    case RULE_INSERT:
		if (i >= control.length()-2) {
		    throw new IllegalArgumentException
			("mangle: insert missing argument in :"+control);
		} else {
		    int pos = char2Int(control.charAt(++i));
		    if (i < 0) {
			throw new IllegalArgumentException
			    ("mangle: insert weird argument in :"+control);
		    }
		    s = new StringBuffer(s)
			.insert(pos,control.charAt(++i)).toString();
		}
		break;
		
		// THE FOLLOWING RULES REQUIRE CLASS MATCHING
	    case RULE_PURGE:	/* @x or @?c */
		if (i == control.length() || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-2))  {
		    throw new IllegalArgumentException
			("mangle: delete missing argument in :"+control);
		    
		} else if (control.charAt(i+1) != RULE_CLASS) {
		    s = purge(s,control.charAt(++i));
		} else {
		    s = polyPurge(s,control.charAt(i+2));
		    i += 2;
		}
		break;
	    case RULE_SUBSTITUTE:	/* sxy || s?cy */
		if (i >= control.length()-2 || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-3))  {
		    throw new IllegalArgumentException
			("mangle: subst missing argument in :"+control);
		} else if (control.charAt(i+1) != RULE_CLASS) {
		    s = s.replace(control.charAt(i+1),control.charAt(i+2));
		    i += 2;
		} else {
		    s = replace(s,control.charAt(i+2),control.charAt(i+3));
		    i += 3;
		}
		break;
	    case RULE_MATCH:	/* /x || /?c */
		if (i == control.length() || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-2))  {
		    throw new IllegalArgumentException
			("mangle: / missing argument in :"+control);
		} else if (control.charAt(i+1) != RULE_CLASS) {
		    if (s.indexOf(control.charAt(++i)) == -1) {
			return null;
		    }
		} else {
		    if (indexOf(s, control.charAt(i+2)) == -1) {
			return null;
		    }
		    i += 2;
		}
		break;
	    case RULE_NOT:		/* !x || !?c */
		if (i == control.length() || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-2))  {
		    throw new IllegalArgumentException
			("mangle: ! missing argument in :"+control);
		} else if (control.charAt(i+1) != RULE_CLASS) {
		    if (s.indexOf(control.charAt(++i)) != -1) {
			return null;
		    }
		} else {
		    if (indexOf(s, control.charAt(i+2)) != -1) {
			return null;
		    }
		    i += 2;
		}
		break;
		/*
		 * alternative use for a boomerang, number 1: a standard throwing
		 * boomerang is an ideal thing to use to tuck the sheets under
		 * the mattress when making your bed.  The streamlined shape of
		 * the boomerang allows it to slip easily 'twixt mattress and
		 * bedframe, and it's curve makes it very easy to hook sheets
		 * into the gap.
		 */
		
	    case RULE_EQUALS:	/* =nx || =n?c */
		if (i >= control.length()-2 || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-3))  {
		    throw new IllegalArgumentException
			("mangle: '=' missing argument in :"+control);
		} else {
		    int pos = char2Int(control.charAt(i+1));
		    if (pos < 0) {
			throw new IllegalArgumentException
			    ("mangle: '='weird argument in :"+control);
		    }
		    if (control.charAt(i+2) != RULE_CLASS) {
			i += 2;
			if (s.charAt(pos) != control.charAt(i)) {
			    return null;
			}
		    } else {
		        i += 3;
			if (!matchClass(s.charAt(i), s.charAt(pos))) {
			    return null;
			}
		    }
		}
		break;
		
	    case RULE_DFIRST:
		s = s.substring(1);
		break;
		
	    case RULE_DLAST:
		s = s.substring(0,s.length()-2);
		break;
		
	    case RULE_MFIRST:
		if (i == control.length() || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-2))  {
		    throw new IllegalArgumentException
			("mangle: '(' missing argument in :"+control);
		} else {
		    if (control.charAt(i+1) != RULE_CLASS) {
			i++;
			if (s.charAt(0) != control.charAt(i)) {
			    return null;
			}
		    } else  {
			i += 2;
			if (!matchClass(control.charAt(i),s.charAt(0))){
			    return null;
			}
		    }
		}
	    case RULE_MLAST:
		if (i == control.length() || 
		    (control.charAt(i+1) == RULE_CLASS && 
		     i == control.length()-2))  {
		    throw new IllegalArgumentException
			("mangle: ')' missing argument in :"+control);
		} else {
		    if (control.charAt(i+1) != RULE_CLASS) {
			i++;
			if (s.charAt(s.length()-1) != control.charAt(i)) {
			    return null;
			}
		    } else  {
			i += 2;
			if (!matchClass(control.charAt(i),
					s.charAt(s.length()-1))) {
			    return null;
			}
		    }
		}
		
	    default:
		throw new IllegalArgumentException
		    ("mangle: unknown command in :"+control);
	    }
	}
	if (s.length() == 0) {
	    return null;
	}
	return s;
    }

    public static final boolean pMatch(String control, String s) {
	if (control.length() != s.length()) {
	    return false;
	}

	for (int i=0;i<s.length() && i<control.length();i++) {
	    if (!matchClass(control.charAt(i),s.charAt(i))) {
		return false;
	    }
	}
	return true;
    }
}
