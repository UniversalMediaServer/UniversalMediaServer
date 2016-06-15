sffmpeg:
	@mkdir -p build && \
	cd build && \
	cmake -DCMAKE_BUILD_TYPE=Release .. && \
	make

buildfiles:
	@mkdir -p build && \
	cd build && \
	cmake -DCMAKE_BUILD_TYPE=Release ..

distclean:
	@rm -rf build

clean:

deb:
	@debuild -i -us -uc -b

debclean:
	@debuild clean
