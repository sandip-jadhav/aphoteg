package photoassociation.qizx;

import java.util.Comparator;

public class AttributeComparator implements Comparator{
	
	private boolean descending = false;

	public AttributeComparator(boolean descending) {
	    this.descending = true;
	  }
	public AttributeComparator() {
	    this.descending = false;
	  }
	
	public int compare(Object str1, Object str2)
	{
		// "valorAtrib|idFoto" -> "9132|559902412" 
		// Primeiro ordena-se pelo valor da feature que estamos a verificar e só depois pelo id da foto
		String[] str1Vect = ((String)str1).split("\\|");
		String[] str2Vect = ((String)str2).split("\\|");
		if(str1Vect[0].isEmpty())
			{
			str1Vect[0] = "0";
			}
		double str1Atrib = Double.parseDouble(str1Vect[0]);
		long  str1Id = Long.parseLong(str1Vect[1]);
		
		
		if(str2Vect[0].isEmpty())
		{
			str2Vect[0] = "0";
		}
        double str2Atrib = Double.parseDouble(str2Vect[0]);
        long str2Id = Long.parseLong(str2Vect[1]);
       
        if(this.descending)
		{
		
	        if(str1Atrib > str2Atrib)
	        {return -1;
	        }
	        else if(str1Atrib < str2Atrib)
	        {return 1;
	        }
	        else if(str1Id > str2Id)
	        {return -1;
	        }
	        else if(str1Id < str2Id)
	        {return 1;
	        }
	        else return 0;
		}
        else
        {
        	if(str1Atrib > str2Atrib)
	        {return 1;
	        }
	        else if(str1Atrib < str2Atrib)
	        {return -1;
	        }
	        else if(str1Id > str2Id)
	        {return 1;
	        }
	        else if(str1Id < str2Id)
	        {return -1;
	        }
	        else return 0;
        }
	}
}
