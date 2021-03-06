/**
 This file is part of NewOrder.

 NewOrder is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 NewOrder is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package no.neworder.prioritization;

import no.neworder.vcs.VCSStatusProvider;
import org.eclipse.jgit.errors.NoWorkTreeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CurrentCodeChanges {

    private final VCSStatusProvider statusProvider;

    public CurrentCodeChanges(VCSStatusProvider statusProvider) {
        this.statusProvider = statusProvider;
    }

    public List<String> prioritize(List<String> localTestClasses)
            throws NoWorkTreeException, IOException {

        List<String> gitStatusPriorityList = new ArrayList<String>();
        gitStatusPriorityList.addAll(statusProvider.getGitStatusPriorityList());
        for(String localTestClass : localTestClasses) {
			if(!gitStatusPriorityList.contains(localTestClass)) {
				gitStatusPriorityList.add(localTestClass);
			}
		}
		return gitStatusPriorityList;
	}
}
