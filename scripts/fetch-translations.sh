#!/bin/bash

declare -A android
android=([cs]=cs [de]=de [es]=es [fr]=fr [hu]=hu [nl]=nl [pt_BR]=pt-rBR [ru_UA]=ru [uk_UA]=uk [zh]=zh)

BASE_DIR=`realpath -L $0 | xargs dirname`/..


function fetch_txt {
	URL=$1
	LANG=$2
	FILE=$3

	TRANSLATIONS=`mktemp`
	curl -sn $1 >$TRANSLATIONS
	diff --ignore-trailing-space -aq $TRANSLATIONS $BASE_DIR/fastlane/metadata/android/en-US/$FILE
	if [[ $? -ne 0 ]]; then
		# translations are not the same as en-us
		mkdir -p $BASE_DIR/fastlane/metadata/android/$LANG
		mv $TRANSLATIONS $BASE_DIR/fastlane/metadata/android/$LANG/$FILE
	fi
	rm -f $TRANSLATIONS
}


for lang in ${!android[@]}
do
	target=../app/src/main/res/values-${android[$lang]}
	mkdir -p $target
	curl -n "https://www.transifex.com/api/2/project/icsx5/resource/icsx5/translation/$lang?file" |sed 's/\.\.\./…/g' >$target/strings.xml

	fetch_txt "https://www.transifex.com/api/2/project/icsx5/resource/full-description/translation/$lang?file" ${android[$lang]} full_description.txt
	fetch_txt "https://www.transifex.com/api/2/project/icsx5/resource/short-description/translation/$lang?file" ${android[$lang]} short_description.txt
done
