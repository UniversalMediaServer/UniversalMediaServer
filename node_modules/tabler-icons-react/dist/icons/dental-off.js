import { objectWithoutProperties as _objectWithoutProperties, extends as _extends } from '../_virtual/_rollupPluginBabelHelpers.js';
import React from 'react';

var _excluded = ["size", "color"];
function DentalOff(_ref) {
  var _ref$size = _ref.size,
      size = _ref$size === void 0 ? 24 : _ref$size,
      _ref$color = _ref.color,
      color = _ref$color === void 0 ? 'currentColor' : _ref$color,
      restProps = _objectWithoutProperties(_ref, _excluded);

  return /*#__PURE__*/React.createElement("svg", _extends({
    xmlns: "http://www.w3.org/2000/svg",
    className: "icon icon-tabler icon-tabler-dental-off",
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    stroke: color,
    strokeWidth: "2",
    fill: "none",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, restProps), /*#__PURE__*/React.createElement("path", {
    stroke: "none",
    d: "M0 0h24v24H0z",
    fill: "none"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.277 15.281c.463 -1.75 .723 -3.844 .723 -6.281c0 -3.74 -1.908 -4.994 -4 -5c-1.423 -.004 -2.92 .911 -4 1.5c-1.074 -.586 -2.583 -1.5 -4 -1.5m-2.843 1.153c-.707 .784 -1.157 2.017 -1.157 3.847c0 4.899 1.056 8.41 2.671 10.537c.573 .756 1.97 .521 2.567 -.236c.398 -.505 .819 -1.439 1.262 -2.801c.292 -.771 .892 -1.504 1.5 -1.5c.602 .004 1.21 .737 1.5 1.5c.443 1.362 .864 2.295 1.262 2.8c.597 .759 1.994 .993 2.567 .237c.305 -.402 .59 -.853 .852 -1.353"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 5.5l3 1.5"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 3l18 18"
  }));
}

export { DentalOff as default };
